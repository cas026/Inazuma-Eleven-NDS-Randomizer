package com.example.randomizer.data

data class UnitBase(
    val fullName: String,
    val nickname: String,
    val elementId: Int,
    val genderId: Int,
    val size: Int,
    val position: Int
) {
    val element: Element? get() = Element.fromId(elementId)
    val gender: Gender?  get() = Gender.fromId(genderId)
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
