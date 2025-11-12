package istic.mob.networkbs

data class Connection (val aId: Int,
                       val bId: Int,
                       var label: String = "",
                       var color: Int = 0xFF000000.toInt(), // default black
                       var strokeWidth: Float = 5f,
                       var controlOffset: Float = 0f, // décalage pour la courbure
                       var thickness: Float = 5f,
                       var curvature: Float = 0f)
{
    constructor(a: Node, b: Node) : this(
        minOf(a.id, b.id),
        maxOf(a.id, b.id)
    )
}