package com.example.randomizer

import com.example.randomizer.data.*
import com.example.randomizer.rom.NdsRom
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Byte-identical roundtrip test: parse → serialize → compare with original.
 *
 * Requires a real ROM. Run with:
 *   ./gradlew :core:test -Prom.path=/path/to/rom.nds
 *
 * The test is silently skipped if rom.path is not set.
 */
class RoundtripTest {

    private val romPath = System.getProperty("rom.path")?.takeIf { it.isNotBlank() }

    @Test
    fun `unitstat dat roundtrip is byte-identical`() {
        assumeTrue(romPath != null, "Skipped — set -Prom.path=/path/to/rom.nds to run")

        val rom     = NdsRom(Path(romPath!!))
        val version = requireVersion(rom)
        val original = loadFile(rom, version, "unitstat.dat")

        val players   = UnitStatParser(version).parse(original)
        val roundtrip = UnitStatSerializer(version).serialize(original, players)

        assertContentEquals(original, roundtrip, buildDiffMessage(original, roundtrip))
    }

    @Test
    fun `unitbase dat roundtrip is byte-identical`() {
        assumeTrue(romPath != null, "Skipped — set -Prom.path=/path/to/rom.nds to run")

        val rom     = NdsRom(Path(romPath!!))
        val version = requireVersion(rom)
        val original = loadFile(rom, version, "unitbase.dat")

        val players   = UnitBaseParser(version).parse(original)
        val roundtrip = UnitBaseSerializer(version).serialize(original, players)

        assertContentEquals(original, roundtrip, buildDiffMessage(original, roundtrip))
    }

    // --- helpers ---

    private fun requireVersion(rom: NdsRom): GameVersion =
        GameVersion.fromGameId(rom.gameId)
            ?: error("Unsupported game ID '${rom.gameId}'. Supported: ${GameVersion.entries.map { it.gameId }}")

    private fun loadFile(rom: NdsRom, version: GameVersion, filename: String): ByteArray =
        rom.getFile(version.dataPath + filename)

    /** Shows the first differing byte offset and its values for quick diagnosis. */
    private fun buildDiffMessage(expected: ByteArray, actual: ByteArray): String {
        if (expected.size != actual.size)
            return "Size mismatch: expected ${expected.size}, got ${actual.size}"
        val first = expected.indices.firstOrNull { expected[it] != actual[it] }
            ?: return "Arrays are equal (no diff found)"
        return "First diff at offset 0x${first.toString(16).uppercase()}: " +
               "expected 0x${(expected[first].toInt() and 0xFF).toString(16).uppercase().padStart(2,'0')}, " +
               "got 0x${(actual[first].toInt() and 0xFF).toString(16).uppercase().padStart(2,'0')}"
    }
}
