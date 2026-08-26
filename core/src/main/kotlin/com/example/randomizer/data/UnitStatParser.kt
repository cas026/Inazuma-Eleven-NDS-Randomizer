package com.example.randomizer.data

import com.example.randomizer.rom.readInt16LE
import com.example.randomizer.rom.readUInt16LE
import com.example.randomizer.rom.readUInt8

class UnitStatParser(private val version: GameVersion) {

    fun parse(data: ByteArray): List<UnitStat> {
        val recordSize = version.statRecordSize
        return (1 until data.size / recordSize)
            .map { parseRecord(data, it * recordSize) }
    }

    private fun parseRecord(data: ByteArray, base: Int): UnitStat {
        fun i16(off: Int) = data.readInt16LE(base + off).toInt()
        fun u8(off: Int)  = data.readUInt8(base + off)
        fun u16(off: Int) = data.readUInt16LE(base + off)

        return UnitStat(
            fp      = StatGrowth(i16(0x00), i16(0x02), i16(0x04)),
            tp      = StatGrowth(i16(0x08), i16(0x0A), i16(0x0C)),
            kick    = StatGrowth(u8(0x10),  u8(0x11),  i16(0x12)),
            body    = StatGrowth(u8(0x14),  u8(0x15),  i16(0x16)),
            guard   = StatGrowth(u8(0x18),  u8(0x19),  i16(0x1A)),
            control = StatGrowth(u8(0x1C),  u8(0x1D),  i16(0x1E)),
            speed   = StatGrowth(u8(0x20),  u8(0x21),  i16(0x22)),
            guts    = StatGrowth(u8(0x24),  u8(0x25),  i16(0x26)),
            stamina = StatGrowth(u8(0x28),  u8(0x29),  i16(0x2A)),
            moves   = (0..3).map { i ->
                val s = 0x2C + i * 4
                MoveSlot(u16(s), u8(s + 2))
            },
            maxTotal = i16(0x3C)
        )
    }
}
