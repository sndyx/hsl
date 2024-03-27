package com.hsc.compiler.ir.ast

class AstMap {

    val cache: HashMap<ULong, Node<*>> = HashMap()

    inline fun <reified T : Any> node(id: ULong): T {
        return cache[id]!!.value as T
    }

    inline fun <reified T : Any> add(value: T, id: NodeId) {
        cache[id.id] = Node(value)
    }

    fun nodes(): List<Node<*>> = cache.values.toList()

    inline fun <reified T : Any> query(): List<T> =
        cache.values
            .filter { it.type == T::class }
            .map { (it as Node<T>).value }

    override fun toString(): String = cache.values.joinToString("\n") {
        it.value.toString()
    }
}