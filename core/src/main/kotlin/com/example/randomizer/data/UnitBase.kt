package com.example.randomizer.data

data class UnitBase(
    val fullName: String,
    val nickname: String,
    val elementId: Int,
    val genderId: Int,
    val size: Int,
    val position: Int
) {
    val element: Element?  get() = Element.fromId(elementId)
    val gender: Gender?    get() = Gender.fromId(genderId)
    val positionRole: Position? get() = Position.fromByte(position)
}

enum class Element(val id: Int, val label: String) {
    AIR(1, "Air"), WOOD(2, "Wood"), FIRE(3, "Fire"), EARTH(4, "Earth");

    companion object {
        fun fromId(id: Int): Element? = entries.firstOrNull { it.id == id }
    }
}

enum class Gender(val id: Int, val label: String) {
    MALE(1, "M"), FEMALE(2, "F");

    companion object {
        fun fromId(id: Int): Gender? = entries.firstOrNull { it.id == id }
    }
}

// Position byte: upper nibble = role (0x20/0x40/0x60/0x80), lower nibble = sub-index (0–8).
enum class Position(val roleBase: Int, val label: String) {
    GK(0x20, "GK"), DF(0x40, "DF"), MF(0x60, "MF"), FW(0x80, "FW");

    companion object {
        fun fromByte(byte: Int): Position? = entries.firstOrNull { byte in it.roleBase..(it.roleBase + 8) }
        val roles = entries.map { it.roleBase }.toIntArray()
    }
}
