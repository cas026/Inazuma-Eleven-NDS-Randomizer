package com.example.randomizer.data

enum class GameVersion(
    val gameId: String,
    val dataPath: String,
    val statRecordSize: Int,
    val baseRecordSize: Int
) {
    IE1_EUR_ES("YEES", "data_iz/logic/sp/", 0x50, 0x60),
    IE1_EUR_EN("YEEP", "data_iz/logic/en/", 0x50, 0x60),
    IE2_EN    ("BEEP", "data_iz/logic/en/", 0x50, 0x60),
    IE2_ES    ("BEES", "data_iz/logic/sp/", 0x50, 0x60),
    IE3_JP    ("BOEJ", "data_iz/logic/",    0x48, 0x68);

    companion object {
        fun fromGameId(id: String): GameVersion? = entries.firstOrNull { it.gameId == id }
    }
}
