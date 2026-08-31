package com.example.randomizer.data

import com.example.randomizer.rom.readInt16LE
import com.example.randomizer.rom.readString
import com.example.randomizer.rom.readUInt8

class UnitBaseParser(private val version: GameVersion) {

    private val stopMarker = byteArrayOf(0x96.toByte(), 0xA2.toByte(), 0x92.toByte(), 0xE8.toByte())

    fun parse(data: ByteArray): List<UnitBase> {
        val recordSize = version.baseRecordSize
        val result = mutableListOf<UnitBase>()

        for (i in 1 until data.size / recordSize) {
            val base = i * recordSize
            if (version == GameVersion.IE3_JP && isStopMarker(data, base)) break
            result.add(parseRecord(data, base))
        }
        return result
    }

    private fun isStopMarker(data: ByteArray, offset: Int): Boolean =
        stopMarker.indices.all { data[offset + it] == stopMarker[it] }

    private fun parseRecord(data: ByteArray, base: Int): UnitBase {
        fun u8(off: Int)           = data.readUInt8(base + off)
        fun i16(off: Int)          = data.readInt16LE(base + off).toInt()
        fun str(off: Int, len: Int) = data.readString(base + off, len)

        return if (version == GameVersion.IE3_JP) UnitBase(
            fullName  = str(0x00, 28),
            nickname  = str(0x1C, 16),
            elementId = u8(0x62),
            genderId  = u8(0x5E),
            size      = u8(0x60),
            position  = 0
            // IE3 model fields intentionally not parsed — model randomization skips IE3
        ) else UnitBase(
            fullName   = str(0x00, 32),
            nickname   = str(0x20, 32),
            elementId  = u8(0x5A),
            genderId   = u8(0x52),
            size       = u8(0x54),
            position   = u8(0x55),
            modelData   = i16(0x44),
            spriteSpecs = i16(0x46),
            rpgHead     = i16(0x48),
            rpgPalette = i16(0x4A),
            modelSpecs = i16(0x4C),
            skinTone   = i16(0x4E)
        )
    }
}
