package istic.mob.networkbs

class Graph  {
    val objects = mutableListOf<Object>()
    val connections = mutableSetOf<Connection>()

    fun addNode(node: Object) {
        objects.add(node)
    }

    fun removeNode(node: Object) {
        objects.remove(node)
        connections.removeIf { it.aId == node.id || it.bId == node.id }
    }

    fun addEdge(a: Object?, b: Object?) {
        if (a == null || b == null) return
        if (a.id == b.id) return
        connections.add(Connection(a, b))
    }

    fun findById(id: Int): Object? {
        return objects.find { it.id == id }
    }

    fun clear() {
        objects.clear()
        connections.clear()
    }
}
