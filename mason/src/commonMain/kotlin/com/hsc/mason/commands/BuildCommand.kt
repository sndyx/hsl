package com.hsc.mason.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.hsc.mason.*
import com.hsc.mason.proc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.peanuuutz.tomlkt.Toml
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.random.Random

class BuildCommand : CliktCommand() {

    private val _path: String? by argument().optional()
    private lateinit var path: Path
    private val toml: Toml = Toml {
        explicitNulls = false
    }
    private val mutex = Mutex()

    private val collectedFiles = mutableListOf<Path>()
    private val fs = FileSystem.SYSTEM

    override fun run(): Unit = runBlocking {
        path = _path?.toPath() ?: "".toPath()
        if (!Git.isInstalled()) { errorMissingGit() } // Ensure git is in PATH!
        if (!Hsc.isInstalled()) { errorMissingHsc() } // Ensure hsc is in PATH!

        val config = build(path)

        val buildPath = path.resolve("build")
        if (!fs.exists(buildPath)) fs.createDirectory(buildPath)

        Hsc.compile(collectedFiles, path.resolve("build"), config)
    }

    private suspend fun build(local: Path, house: House? = null): House = coroutineScope {
        val config = if (house != null) house else {
            val houseFile = local.resolve("House.toml")
            runCatching {
                toml.decodeFromString<House>(fs.read(houseFile) { readUtf8() })
            }.getOrElse { errorInvalidManifest(it) }
        }

        val lockFile = local.resolve("House.lock")
        val prevLock = if (FileSystem.SYSTEM.exists(lockFile)) {
            runCatching {
                toml.decodeFromString<Lock>(fs.read(lockFile) { readUtf8() })
            }.getOrElse { errorInvalidLockfile() }
        } else Lock(mutableListOf())

        val lock = Lock(mutableListOf())

        config.dependencies?.map { (name, value) ->
            async {
                fetchDependency(lock, prevLock, name, value)
            }
        }?.awaitAll()?.forEach {
            build(path.resolve("build/libs").resolve(it.pkg.name), it)
        }

        FileSystem.SYSTEM.write(lockFile) {
            writeUtf8(toml.encodeToString(lock))
        }

        collectSrc(local)

        config
    }

    private fun collectSrc(path: Path) {
        val src = path.resolve("src")
        if (fs.exists(src)) {
            fs.listRecursively(src).forEach {
                if (it.name.endsWith(".hsl")) collectedFiles.add(it)
            }
        }
    }

    private suspend fun fetchDependency(
        lock: Lock,
        prevLock: Lock,
        name: String,
        dependency: Dependency
    ): House = coroutineScope {
        if (dependency.git.contains("://")) errorProtocolURL(dependency.git)
        val git = "https://" + dependency.git.removePrefix(".git") + ".git"
        // val url = runCatching { URL(git) }.getOrElse { errorMalformedURL(git) }

        val commit = Git.latest(git, dependency.version)
        val captured = prevLock.dependencies.find { it.name == name }
        if (
            captured == null || // We have no local save
            captured.url != git || // Remote has changed
            commit != captured.commit // Commit doesn't match
        ) {
            // Dependency is not up-to-date

            // Clone remote to temp directory
            val temp = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("mason_repo${Random.nextLong()}")
            Git.clone(temp, git, dependency.version)

            if (!fs.exists(temp.resolve("House.toml"))) { errorNotAPackage(name) }

            val manifest = runCatching {
                toml.decodeFromString<House>(fs.read(temp.resolve("House.toml")) { readUtf8() })
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

            val newPath = path.resolve("build/libs").resolve(name)
            runCatching {
                fs.deleteRecursively(newPath)
            }.onFailure { errorCannotDelete() }

            move(temp, newPath)

            runCatching { // Why? Why does git do this?
                fs.deleteRecursively(temp)
            }

            manifest
        } else {
            val depPath = path.resolve("build/libs").resolve(name)

            lock.dependencies.add(prevLock.dependencies.find { it.name == name }!!)

            runCatching {
                toml.decodeFromString<House>(fs.read(depPath.resolve("House.toml")) { readUtf8() })
            }.getOrElse { errorInvalidManifest(it) }
        }
    }

    private fun move(source: Path, destination: Path) {
        fs.createDirectories(destination)
        val dfs = mutableListOf(source)
        while (dfs.isNotEmpty()) {
            val path = dfs.removeLast()
            val destPath = destination.resolve(path.relativeTo(source))
            fs.createDirectory(destPath)
            fs.list(path).forEach {
                if (fs.metadata(it).isDirectory) {
                    if (it.name != ".git") {
                        dfs.add(it)
                    }
                } else {
                    runCatching {
                        fs.atomicMove(it, destPath.resolve(it.name))
                    }
                }
            }
        }
    }

}