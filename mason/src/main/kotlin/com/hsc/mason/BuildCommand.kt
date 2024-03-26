package com.hsc.mason

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.peanuuutz.tomlkt.Toml
import java.io.IOException
import java.io.UncheckedIOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*


class BuildCommand : CliktCommand() {

    private val path: Path by argument().path()
    private val toml: Toml = Toml {
        explicitNulls = false
    }
    private val mutex = Mutex()

    override fun run(): Unit = runBlocking {
        if (!Git.isInstalled()) { errorMissingGit() } // Ensure git is in PATH!

       build(path)
    }


    private suspend fun build(local: Path, house: House? = null): Unit = coroutineScope {
        println("Building $local")

        val config = if (house != null) house else {
            val houseFile = local.resolve("House.toml")
            runCatching {
                toml.decodeFromString<House>(houseFile.readText())
            }.getOrElse { errorInvalidManifest(it) }
        }

        val lockFile = local.resolve("House.lock")
        val prevLock = if (lockFile.exists()) {
            runCatching {
                toml.decodeFromString<Lock>(lockFile.readText())
            }.getOrElse { errorInvalidLockfile() }
        } else Lock(mutableListOf())

        val lock = Lock(mutableListOf())

        config.dependencies?.map { (name, value) ->
            async {
                fetchDependency(lock, prevLock, name, value)
            }
        }?.awaitAll()?.forEach {
            build(path.resolve("libs").resolve(it.pkg.name), it)
        }

        lockFile.writeText(toml.encodeToString(lock))
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun fetchDependency(
        lock: Lock,
        prevLock: Lock,
        name: String,
        dependency: Dependency
    ): House = coroutineScope {
        if (dependency.git.contains("://")) errorProtocolURL(dependency.git)
        val git = "https://" + dependency.git.removePrefix(".git") + ".git"
        val url = runCatching { URL(git) }.getOrElse { errorMalformedURL(git) }

        val commit = Git.latest(url, dependency.version)
        val captured = prevLock.dependencies.find { it.name == name }
        if (
            captured == null || // We have no local save
            captured.url != git || // Remote has changed
            commit != captured.commit // Commit doesn't match
        ) {
            // Dependency is not up-to-date

            // Clone remote to temp directory
            val temp = Files.createTempDirectory("mason_repo").toAbsolutePath()
            temp.toFile().deleteOnExit()
            Git.clone(temp.toAbsolutePath(), url, dependency.version)

            if (temp.resolve("House.toml").notExists()) { errorNotAPackage(name) }

            val manifest = runCatching {
                toml.decodeFromString<House>(temp.resolve("House.toml").readText())
            }.getOrElse { errorInvalidManifest(it) }

            if (manifest.pkg.name != name) errorWrongName(name, manifest.pkg.name)

            // Add new capture to lockfile
            val newCapture = CapturedDependency(name, git, commit)

            // Check if another dependency was already added to the lockfile
            mutex.withLock<Unit> {
                val new = lock.dependencies.find { it.name == name }
                if (new == null) {
                    lock.dependencies.add(newCapture)
                } else if (new.commit != commit) {
                    errorDependencyConflict(name)
                }
            }

            val newPath = path.resolve("libs").resolve(name)
            runCatching {
                newPath.deleteRecursively()
            }.onFailure { errorCannotDelete() }

            move(temp, newPath)

            manifest
        } else {
            val depPath = path.resolve("libs").resolve(name)

            lock.dependencies.add(prevLock.dependencies.find { it.name == name }!!)

            runCatching {
                toml.decodeFromString<House>(depPath.resolve("House.toml").readText())
            }.getOrElse { errorInvalidManifest(it) }
        }
    }

    private fun move(source: Path, destination: Path) {
        destination.createParentDirectories()
        val dfs = mutableListOf(source)
        while (dfs.isNotEmpty()) {
            val path = dfs.removeLast()
            val destPath = destination.resolve(path.relativeTo(source))
            destPath.createDirectory()
            path.listDirectoryEntries().forEach {
                if (it.isDirectory()) {
                    if (it.name != ".git") {
                        dfs.add(it)
                    }
                } else {
                    runCatching { it.moveTo(destPath.resolve(it.fileName)) }
                }
            }
        }
    }

}