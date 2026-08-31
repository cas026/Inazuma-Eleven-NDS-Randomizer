package com.example.randomizer.data

import com.example.randomizer.rom.writeInt16LE
import com.example.randomizer.rom.writeString
import com.example.randomizer.rom.writeUInt8

class UnitBaseSerializer(private val version: GameVersion) {

    /**
     * Reconstructs the full unitbase.dat file.
     * Starts from [original] so that visual/model fields and padding bytes we do not
     * track in UnitBase are preserved exactly, then overwrites the parsed fields.
     */
    fun serialize(original: ByteArray, players: List<UnitBase>): ByteArray {
        val result = original.copyOf()
        val recordSize = version.baseRecordSize
        players.forEachIndexed { i, base ->
            writeRecord(result, (i + 1) * recordSize, base)
        }
        return result
    }

    private fun writeRecord(out: ByteArray, base: Int, player: UnitBase) {
        if (version == GameVersion.IE3_JP) {
            out.writeString(base + 0x00, 28, player.fullName)
            out.writeString(base + 0x1C, 16, player.nickname)
            out.writeUInt8 (base + 0x5E, player.genderId)
            out.writeUInt8 (base + 0x60, player.size)
            out.writeUInt8 (base + 0x62, player.elementId)
        } else {
            out.writeString (base + 0x00, 32, player.fullName)
            out.writeString (base + 0x20, 32, player.nickname)
            out.writeInt16LE(base + 0x44, player.modelData)
            out.writeInt16LE(base + 0x46, player.spriteSpecs)
            out.writeInt16LE(base + 0x48, player.rpgHead)
            out.writeInt16LE(base + 0x4A, player.rpgPalette)
            out.writeInt16LE(base + 0x4C, player.modelSpecs)
            out.writeInt16LE(base + 0x4E, player.skinTone)
            out.writeUInt8  (base + 0x52, player.genderId)
            out.writeUInt8  (base + 0x54, player.size)
            out.writeUInt8  (base + 0x55, player.position)
            out.writeUInt8  (base + 0x5A, player.elementId)
        }
    }
}
