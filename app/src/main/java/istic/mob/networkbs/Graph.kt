package istic.mob.networkbs
import com.google.gson.Gson

import org.json.JSONArray
import org.json.JSONObject
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

//    fun toJson(): String {
//        return Gson().toJson(this)
//    }

    companion object {
        fun fromJson(json: String): Graph {
            return Gson().fromJson(json, Graph::class.java)
        }
    }

    // -----------------------------
    // JSON SAVE
    // -----------------------------
    fun toJson(): JSONObject {
        val root = JSONObject()

        val arrNodes = JSONArray()
        for (n in nodes) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("label", n.label)
            o.put("x", n.x)
            o.put("y", n.y)
            o.put("color", n.color)
            arrNodes.put(o)
        }

        val arrEdges = JSONArray()
        for (c in connections) {
            val o = JSONObject()
            o.put("aId", c.aId)
            o.put("bId", c.bId)
            o.put("label", c.label)
            o.put("color", c.color)
            o.put("strokeWidth", c.strokeWidth)
            o.put("controlOffset", c.controlOffset)
            arrEdges.put(o)
        }

        root.put("nodes", arrNodes)
        root.put("connections", arrEdges)

        return root
    }

    // -----------------------------
    // JSON LOAD
    // -----------------------------
    fun fromJson(json: JSONObject) {
        clear()

        val arrNodes = json.getJSONArray("nodes")
        for (i in 0 until arrNodes.length()) {
            val o = arrNodes.getJSONObject(i)
            val n = Node(
                label = o.getString("label"),
                x = o.getDouble("x").toFloat(),
                y = o.getDouble("y").toFloat(),
                id = o.getInt("id")
            )
            n.color = o.getInt("color")
            nodes.add(n)
        }

        val arrEdges = json.getJSONArray("connections")
        for (i in 0 until arrEdges.length()) {
            val o = arrEdges.getJSONObject(i)
            val c = Connection(
                aId = o.getInt("aId"),
                bId = o.getInt("bId"),
                label = o.getString("label"),
                color = o.getInt("color"),
                strokeWidth = o.getDouble("strokeWidth").toFloat(),
                controlOffset = o.getDouble("controlOffset").toFloat()
            )
            connections.add(c)
        }
    }
}
