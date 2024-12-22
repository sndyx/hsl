package com.hsc.compiler.pretty

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Condition
import com.hsc.compiler.ir.action.Function
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

fun prettyPrintActions(t: Terminal, functions: List<Function>) {
    val encoder = PPActionsEncoder(t, 0)
    functions.forEach {
        t.println("${blue("function")} ${bold(it.name)}:")
        it.actions.forEach { action ->
            encoder.encodeSerializableValue(Action.serializer(), action)
        }
    }
}

fun prettyPrintAction(t: Terminal, action: Action, indent: Int = 0) {
    val encoder = PPActionsEncoder(t, indent)
    encoder.encodeSerializableValue(Action.serializer(), action)
}

fun prettyPrintCondition(t: Terminal, condition: Condition, indent: Int = 0) {
    val encoder = PPActionsEncoder(t, indent)
    encoder.encodeSerializableValue(Condition.serializer(), condition)
}

@OptIn(ExperimentalSerializationApi::class)
class PPActionsEncoder(val t: Terminal, startIndent: Int) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()


    private var indent = maxOf(0, startIndent - 1)
    private val i: String get() = "  ".repeat(indent)

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) = with(t) {
        val name = descriptor.getElementName(index)
        if (descriptor.kind != StructureKind.LIST) {
            if (serializer.descriptor.kind == StructureKind.LIST) {
                println(gray("$i$name:"))
            } else {
                print(gray("$i$name = "))
            }
        }
        if (name != "item") serializer.serialize(this@PPActionsEncoder, value)
        else println(white("{${(gray.bg + brightWhite)("...")}}")) // Cannot (and should not, frankly) display items
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        when (descriptor.kind) {
            PolymorphicKind.SEALED -> return Polymorphic().also { indent++ }
            StructureKind.LIST -> {}
            else -> indent++
        }
        return this
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (descriptor.kind != StructureKind.LIST) indent--
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = with(t) {
        val name = descriptor.getElementName(index)
        print(gray("$i$name = "))
        true
    }

    override fun encodeBoolean(value: Boolean) = with(t) { println(blue("$value")) }
    override fun encodeInt(value: Int) = with(t) { println(white("$value")) }
    override fun encodeLong(value: Long) = with(t) { println(white("$value")) }
    override fun encodeFloat(value: Float) = with(t) { println(white("$value")) }
    override fun encodeDouble(value: Double) = with(t) { println(white("$value")) }
    override fun encodeString(value: String) = with(t) { println((green + dim)("\"$value\"")) }
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = with(t) {
        println(white(enumDescriptor.getElementName(index).lowercase()))
    }

    /**
     * Encodes polymorphic values as a regular object where the key is their type.
     */
    inner class Polymorphic : AbstractEncoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
            this@PPActionsEncoder.also { indent++ }

        override fun endStructure(descriptor: SerialDescriptor) { indent-- }

        override fun encodeString(value: String) {
            println(bold("$i${value.lowercase()}:"))
        }
    }

}