package istic.mob.networkbs

data class Connection (val aId: Int, val bId: Int) {
    constructor(a: Object, b: Object) : this(
        minOf(a.id, b.id),
        maxOf(a.id, b.id)
    )
}