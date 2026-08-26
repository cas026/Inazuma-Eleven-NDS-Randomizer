package com.example.randomizer.data

enum class Game { IE1, IE2, IE3 }

enum class GameVersion(
    val gameId: String,
    val dataPath: String,
    val statRecordSize: Int,
    val baseRecordSize: Int,
    val displayName: String,
    val game: Game
) {
    // ── Inazuma Eleven ────────────────────────────────────────────────────────
    IE1_EN("YEEP", "data_iz/logic/en/", 0x50, 0x60, "Inazuma Eleven (EN)", Game.IE1),
    IE1_FR("YEEF", "data_iz/logic/fr/", 0x50, 0x60, "Inazuma Eleven (FR)", Game.IE1),
    IE1_DE("YEED", "data_iz/logic/de/", 0x50, 0x60, "Inazuma Eleven (DE)", Game.IE1),
    IE1_IT("YEEI", "data_iz/logic/it/", 0x50, 0x60, "Inazuma Eleven (IT)", Game.IE1),
    IE1_JA("YEEJ", "data_iz/logic/",    0x50, 0x60, "Inazuma Eleven (JA)", Game.IE1),
    IE1_ES("YEES", "data_iz/logic/sp/", 0x50, 0x60, "Inazuma Eleven (ES)", Game.IE1),

    // ── Inazuma Eleven 2 ──────────────────────────────────────────────────────
    IE2_EN_FIRESTORM("BEEP", "data_iz/logic/en/", 0x50, 0x60, "Inazuma Eleven 2: Firestorm (EN)",                       Game.IE2),
    IE2_EN_BLIZZARD ("BEBP", "data_iz/logic/en/", 0x50, 0x60, "Inazuma Eleven 2: Blizzard (EN)",                        Game.IE2),
    IE2_FR_FIRESTORM("BEEF", "data_iz/logic/fr/", 0x50, 0x60, "Inazuma Eleven 2: Tempête de Feu (FR)",             Game.IE2),
    IE2_FR_BLIZZARD ("BEBF", "data_iz/logic/fr/", 0x50, 0x60, "Inazuma Eleven 2: Tempête de Glace (FR)",           Game.IE2),
    IE2_DE_FIRESTORM("BEED", "data_iz/logic/de/", 0x50, 0x60, "Inazuma Eleven 2: Feuersturm (DE)",                      Game.IE2),
    IE2_DE_BLIZZARD ("BEBD", "data_iz/logic/de/", 0x50, 0x60, "Inazuma Eleven 2: Eissturm (DE)",                        Game.IE2),
    IE2_IT_FIRESTORM("BEEI", "data_iz/logic/it/", 0x50, 0x60, "Inazuma Eleven 2: Tempesta di Fuoco (IT)",               Game.IE2),
    IE2_IT_BLIZZARD ("BEBI", "data_iz/logic/it/", 0x50, 0x60, "Inazuma Eleven 2: Bufera di Neve (IT)",                  Game.IE2),
    IE2_JA_FIRESTORM("BEEJ", "data_iz/logic/",    0x50, 0x60, "Inazuma Eleven 2: Kyoui no Shinryakusha - Fire (JA)",    Game.IE2),
    IE2_JA_BLIZZARD ("BEBJ", "data_iz/logic/",    0x50, 0x60, "Inazuma Eleven 2: Kyoui no Shinryakusha - Blizzard (JA)",Game.IE2),
    IE2_ES_FIRESTORM("BEES", "data_iz/logic/sp/", 0x50, 0x60, "Inazuma Eleven 2: Tormenta de Fuego (ES)",               Game.IE2),
    IE2_ES_BLIZZARD ("BEBS", "data_iz/logic/sp/", 0x50, 0x60, "Inazuma Eleven 2: Ventisca Eterna (ES)",                 Game.IE2),

    // ── Inazuma Eleven 3 ──────────────────────────────────────────────────────
    IE3_JP("BOEJ", "data_iz/logic/", 0x48, 0x68, "Inazuma Eleven 3 (JP)", Game.IE3);

    companion object {
        fun fromGameId(id: String): GameVersion? = entries.firstOrNull { it.gameId == id }
    }
}
