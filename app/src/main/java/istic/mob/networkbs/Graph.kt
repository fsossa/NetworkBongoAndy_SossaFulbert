package istic.mob.networkbs
import com.google.gson.Gson
class Graph (
    val nodes: MutableList<Node> = mutableListOf(),
    val connections: MutableList<Connection> = mutableListOf()
) {
    // val objects = mutableListOf<Object>()
    // val connections = mutableSetOf<Connection>()

    fun addNode(node: Node) {
        if (node.id == 0) node.id = Node.nextId()
        nodes.add(node)
    }

    fun removeNode(node: Node) {
        nodes.remove(node)
        connections.removeAll { it.aId == node.id || it.bId == node.id }
    }
    fun addConnection(a: Node, b: Node, label: String = "") {
        if (a.id == b.id) return
        // empêcher doublons indépendamment de l'ordre
        val existing = connections.any { (it.aId == a.id && it.bId == b.id) || (it.aId == b.id && it.bId == a.id) }
        if (existing) return
        connections.add(Connection(minOf(a.id, b.id), maxOf(a.id, b.id), label))
    }

    fun addEdge(a: Node?, b: Node?) {
        if (a == null || b == null) return
        if (a.id == b.id) return
        connections.add(Connection(a, b))
    }

    fun findById(id: Int): Node? {
        return nodes.find { it.id == id }
    }

    fun clear() {
        nodes.clear()
        connections.clear()
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): Graph {
            return Gson().fromJson(json, Graph::class.java)
        }
    }
}
