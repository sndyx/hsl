package com.hsc.mason

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import net.peanuuutz.tomlkt.*

@Serializable
data class House(
    @SerialName("package")
    val pkg: Package,
    val dependencies: Map<String, Dependency>?,
    val features: Map<String, Feature>?,
)

@Serializable
data class Package(
    val name: String,
    val version: String,
    val mode: Mode?,
    val target: Target?,
)

@Serializable
sealed class Feature {
    @Serializable
    @SerialName("event")
    data class Event(val name: String) : Feature()
    @Serializable
    @SerialName("button")
    data class Button(val location: Location) : Feature()
    @Serializable
    @SerialName("pad")
    data class Pad(val location: Location) : Feature()
}

@Serializable
enum class Mode {
    @SerialName("normal") Normal,
    @SerialName("strict") Strict,
    @SerialName("optimize") Optimize;
}

@Serializable
enum class Target {
    @SerialName("normal") Normal,
    @SerialName("housing+") HousingPlus;
}

@Serializable(with = DependencySerializer::class)
data class Dependency(
    val git: String,
    val version: String?,
)

@Serializable(with = LocationSerializer::class)
data class Location(val x: Int, val y: Int, val z: Int)

private object DependencySerializer : TomlContentPolymorphicSerializer<Dependency>(Dependency::class) {
    override fun selectDeserializer(element: TomlElement): DeserializationStrategy<Dependency> {
        return if (element is TomlTable) {
            DependencyDeserializationStrategy
        } else {
            DependencyStringDeserializationStrategy
        }
    }
}

private object DependencyDeserializationStrategy : DeserializationStrategy<Dependency> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Dependency") {
        element<String>("repo")
        element<String?>("version")
    }
    override fun deserialize(decoder: Decoder): Dependency {
        // ktoml bugs out trying to decodeStructure here, so just going to
        // manually retrieve Toml elements...
        val table = decoder.asTomlDecoder().decodeTomlElement() as TomlTable
        val git = table["git"]?.content as String
        val version = table["version"]?.content as? String
        table.keys.find { it != "git" && it != "version" }?.let {
            error(it)
        }

        return Dependency(git, version)
    }

}

private object DependencyStringDeserializationStrategy : DeserializationStrategy<Dependency> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder): Dependency {
        val repository = decoder.decodeString()
        return Dependency(repository, null)
    }
}

@ExperimentalSerializationApi
private object LocationSerialDescriptor : SerialDescriptor {
    override val elementsCount: Int = 3
    override val kind: SerialKind = StructureKind.LIST
    override val serialName: String = "Location"
    override fun getElementAnnotations(index: Int): List<Annotation> = emptyList()
    override fun getElementDescriptor(index: Int): SerialDescriptor = Int.serializer().descriptor
    override fun getElementIndex(name: String): Int = name.toInt()
    override fun getElementName(index: Int): String = index.toString()
    override fun isElementOptional(index: Int): Boolean = false
}

private object LocationSerializer : KSerializer<Location> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = LocationSerialDescriptor
    override fun deserialize(decoder: Decoder): Location {
        return decoder.decodeStructure(descriptor) {
            val x = decodeIntElement(descriptor, 0)
            val y = decodeIntElement(descriptor, 1)
            val z = decodeIntElement(descriptor, 2)
            Location(x, y, z).also { endStructure(descriptor) }
        }
    }
    override fun serialize(encoder: Encoder, value: Location) = Unit
}