package com.example.randomizer.data

import com.example.randomizer.rom.readInt16LE
import com.example.randomizer.rom.readUInt8

class CommandDatParser(private val commandRecordSize: Int = 0x1C) {

    companion object {
        private const val UNITCALC_HEADER      = 0x20
        private const val UNITCALC_RECORD_SIZE = 0x20
        private const val STR_SLOT_SIZE        = 96

        fun readNames(data: ByteArray, isJapanese: Boolean): Map<Int, String> {
            val charset = if (isJapanese) charset("Shift_JIS") else Charsets.UTF_8
            val result  = mutableMapOf<Int, String>()
            for (i in 0 until data.size / STR_SLOT_SIZE) {
                val off = i * STR_SLOT_SIZE
                var end = off
                while (end < data.size && data[end] != 0.toByte()) end++
                val name = String(data, off, end - off, charset).trim()
                if (name.isNotBlank()) result[i] = name
            }
            return result
        }
    }

    fun parse(commandDat: ByteArray, unitcalcDat: ByteArray?, names: Map<Int, String> = emptyMap()): List<MoveData> {
        val count  = commandDat.size / commandRecordSize
        val result = mutableListOf<MoveData>()
        for (id in 1 until count) {
            val base  = id * commandRecordSize
            val type  = commandDat.readUInt8(base + 0x0)
            if (type !in MoveType.DRIBBLE..MoveType.KEEPER) continue
            val power = commandDat.readUInt8(base + 0x6)
            val limit = readLimit(unitcalcDat, id)
            result += MoveData(id = id, type = type, power = power, limit = limit, name = names[id] ?: "")
        }
        return result
    }

    fun maxLimit(unitcalcDat: ByteArray?, validIds: List<Int>): Int =
        if (unitcalcDat == null) Int.MAX_VALUE
        else validIds.maxOfOrNull { readLimit(unitcalcDat, it) } ?: Int.MAX_VALUE

    private fun readLimit(unitcalcDat: ByteArray?, id: Int): Int {
        if (unitcalcDat == null) return Int.MAX_VALUE
        val offset = UNITCALC_HEADER + id * UNITCALC_RECORD_SIZE
        return if (offset + 2 <= unitcalcDat.size)
            unitcalcDat.readInt16LE(offset).toInt()
        else Int.MAX_VALUE
    }
}
