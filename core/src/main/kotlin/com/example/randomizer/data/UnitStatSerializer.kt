package com.example.randomizer.data

import com.example.randomizer.rom.writeInt16LE
import com.example.randomizer.rom.writeUInt8

class UnitStatSerializer(private val version: GameVersion) {

    /**
     * Reconstructs the full unitstat.dat file.
     * Starts from [original] so that unknown/padding bytes are preserved exactly,
     * then overwrites every parsed field with the (possibly modified) values from [players].
     * Player at index i maps to record i+1 (record 0 is the null entry).
     */
    fun serialize(original: ByteArray, players: List<UnitStat>): ByteArray {
        val result = original.copyOf()
        val recordSize = version.statRecordSize
        players.forEachIndexed { i, stat ->
            writeRecord(result, (i + 1) * recordSize, stat)
        }
        return result
    }

    private fun writeRecord(out: ByteArray, base: Int, stat: UnitStat) {
        // FP block — bytes 0x06-0x07 (unknown) left as-is from original
        out.writeInt16LE(base + 0x00, stat.fp.min)
        out.writeInt16LE(base + 0x02, stat.fp.max)
        out.writeInt16LE(base + 0x04, stat.fp.growthRate)
        // TP block — bytes 0x0E-0x0F (unknown) left as-is from original
        out.writeInt16LE(base + 0x08, stat.tp.min)
        out.writeInt16LE(base + 0x0A, stat.tp.max)
        out.writeInt16LE(base + 0x0C, stat.tp.growthRate)
        // Combat stats
        out.writeUInt8  (base + 0x10, stat.kick.min)
        out.writeUInt8  (base + 0x11, stat.kick.max)
        out.writeInt16LE(base + 0x12, stat.kick.growthRate)
        out.writeUInt8  (base + 0x14, stat.body.min)
        out.writeUInt8  (base + 0x15, stat.body.max)
        out.writeInt16LE(base + 0x16, stat.body.growthRate)
        out.writeUInt8  (base + 0x18, stat.guard.min)
        out.writeUInt8  (base + 0x19, stat.guard.max)
        out.writeInt16LE(base + 0x1A, stat.guard.growthRate)
        out.writeUInt8  (base + 0x1C, stat.control.min)
        out.writeUInt8  (base + 0x1D, stat.control.max)
        out.writeInt16LE(base + 0x1E, stat.control.growthRate)
        out.writeUInt8  (base + 0x20, stat.speed.min)
        out.writeUInt8  (base + 0x21, stat.speed.max)
        out.writeInt16LE(base + 0x22, stat.speed.growthRate)
        out.writeUInt8  (base + 0x24, stat.guts.min)
        out.writeUInt8  (base + 0x25, stat.guts.max)
        out.writeInt16LE(base + 0x26, stat.guts.growthRate)
        out.writeUInt8  (base + 0x28, stat.stamina.min)
        out.writeUInt8  (base + 0x29, stat.stamina.max)
        out.writeInt16LE(base + 0x2A, stat.stamina.growthRate)
        // Move slots — padding byte at (s+3) left as-is from original
        stat.moves.forEachIndexed { i, move ->
            val s = 0x2C + i * 4
            out.writeInt16LE(base + s,     move.id)
            out.writeUInt8  (base + s + 2, move.unlockLevel)
        }
        // Max total — trailing unknown/padding left as-is from original
        out.writeInt16LE(base + 0x3C, stat.maxTotal)
    }
}
