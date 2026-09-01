package com.example.randomizer

import com.example.randomizer.data.*
import com.example.randomizer.randomizer.*
import kotlin.test.*

class MoveRandomizerTest {

    private val version = GameVersion.IE1_EN

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeMove(id: Int, type: Int = MoveType.SHOOT, power: Int = 50, limit: Int = 100) =
        MoveData(id = id, type = type, power = power, limit = limit)

    private fun makeStat(vararg moveIds: Int) = UnitStat(
        fp      = StatGrowth(100, 300, 50),
        tp      = StatGrowth(50, 200, 30),
        kick    = StatGrowth(50, 99, 100),
        body    = StatGrowth(40, 85, 80),
        guard   = StatGrowth(45, 90, 90),
        control = StatGrowth(55, 95, 110),
        speed   = StatGrowth(60, 100, 120),
        guts    = StatGrowth(35, 80, 70),
        stamina = StatGrowth(50, 90, 100),
        moves   = moveIds.map { MoveSlot(it, 1) }.let { slots ->
            slots + List(4 - slots.size) { MoveSlot(0, 0) }
        }.take(4),
        maxTotal = 500
    )

    private fun makeGkPlayer() = UnitBase(
        fullName = "GK", nickname = "GK",
        elementId = 0, genderId = 0, size = 0, position = Position.GK.roleBase
    )

    private fun makeOutfieldPlayer() = UnitBase(
        fullName = "FW", nickname = "FW",
        elementId = 0, genderId = 0, size = 0, position = Position.FW.roleBase
    )

    // move pool: a handful of non-GK moves + a few GK moves
    private val gkMove    = makeMove(10, type = MoveType.KEEPER)
    private val catchMove = makeMove(11, type = MoveType.NORMAL_CATCH)
    private val ie1Move   = makeMove(30, type = MoveType.SHOOT)   // id 30 ≤ 66 → IE1
    private val ie2Move   = makeMove(80, type = MoveType.SHOOT)   // id 80 > 66 → IE2
    private val ie1Move2  = makeMove(50, type = MoveType.DRIBBLE)
    private val ie2Move2  = makeMove(70, type = MoveType.BLOCK)

    private val allMoves = listOf(gkMove, catchMove, ie1Move, ie2Move, ie1Move2, ie2Move2)

    private fun config(
        moveMode: MoveMode = MoveMode.RANDOM_TOTALLY,
        seed: Long = 42L,
        samePositionMove: Boolean = false,
        storyMoveProtection: Boolean = false,  // off by default so index-0 players are not skipped
        randomMoveLevel: Boolean = false,
        maxMoveLevel: Int = 99,
        limitOfSkill: Boolean = false,
        maxMoveLimit: Int = Int.MAX_VALUE,
        onlyIe1Moves: Boolean = false,
        onlyIe2Moves: Boolean = false
    ) = RandomizerConfig(
        seed = seed, moveMode = moveMode,
        samePositionMove = samePositionMove,
        storyMoveProtection = storyMoveProtection,
        randomMoveLevel = randomMoveLevel,
        maxMoveLevel = maxMoveLevel,
        limitOfSkill = limitOfSkill,
        maxMoveLimit = maxMoveLimit,
        onlyIe1Moves = onlyIe1Moves,
        onlyIe2Moves = onlyIe2Moves
    )

    // ── NOT_CHANGED ───────────────────────────────────────────────────────────

    @Test fun `NOT_CHANGED returns input unchanged`() {
        val stats   = List(10) { makeStat(10, 20) }
        val players = List(10) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(moveMode = MoveMode.NOT_CHANGED), version)
            .randomize(stats, players, allMoves)
        assertEquals(stats, result)
    }

    // ── IE3 guard ─────────────────────────────────────────────────────────────

    @Test fun `IE3 version returns input unchanged regardless of mode`() {
        val stats   = List(10) { makeStat(10, 20) }
        val players = List(10) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(), GameVersion.IE3_JP)
            .randomize(stats, players, allMoves)
        assertEquals(stats, result)
    }

    // ── determinism ───────────────────────────────────────────────────────────

    @Test fun `same seed produces identical output`() {
        val stats   = List(20) { makeStat(5, 15) }
        val players = List(20) { makeOutfieldPlayer() }
        assertEquals(
            MoveRandomizer(config(seed = 99L), version).randomize(stats, players, allMoves),
            MoveRandomizer(config(seed = 99L), version).randomize(stats, players, allMoves)
        )
    }

    @Test fun `different seeds produce different output`() {
        val stats   = List(20) { makeStat(5, 15) }
        val players = List(20) { makeOutfieldPlayer() }
        assertNotEquals(
            MoveRandomizer(config(seed = 1L), version).randomize(stats, players, allMoves),
            MoveRandomizer(config(seed = 2L), version).randomize(stats, players, allMoves)
        )
    }

    // ── empty slots are never filled ──────────────────────────────────────────

    @Test fun `empty move slots stay empty after randomization`() {
        val stat    = makeStat(10, 0, 0, 0)  // only slot 0 has a move
        val players = listOf(makeOutfieldPlayer())
        val result  = MoveRandomizer(config(), version).randomize(listOf(stat), players, allMoves)
        assertTrue(result[0].moves[1].isEmpty, "slot 1 should stay empty")
        assertTrue(result[0].moves[2].isEmpty, "slot 2 should stay empty")
        assertTrue(result[0].moves[3].isEmpty, "slot 3 should stay empty")
    }

    @Test fun `non-empty slots get new move IDs from the pool`() {
        val stat    = makeStat(10, 20)
        val players = listOf(makeOutfieldPlayer())
        val result  = MoveRandomizer(config(), version).randomize(listOf(stat), players, allMoves)
        val ids     = allMoves.map { it.id }.toSet()
        assertFalse(result[0].moves[0].isEmpty)
        assertFalse(result[0].moves[1].isEmpty)
        assertTrue(result[0].moves[0].id in ids, "slot 0 id not from pool")
        assertTrue(result[0].moves[1].id in ids, "slot 1 id not from pool")
    }

    // ── story protection ──────────────────────────────────────────────────────

    @Test fun `story players are skipped when storyMoveProtection=true`() {
        val story   = makeStat(5, 15)
        val normal  = makeStat(5, 15)
        val stats   = List(12) { i -> if (i < 6) story else normal }  // 0–5 are story for IE1
        val players = List(12) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(storyMoveProtection = true), version)
            .randomize(stats, players, allMoves)
        // story indices 0–5: moves unchanged
        (0 until 6).forEach { i ->
            assertEquals(story.moves, result[i].moves, "story player $i moves changed")
        }
        // non-story indices may differ (moves from pool)
        val allIds = allMoves.map { it.id }.toSet()
        (6 until 12).forEach { i ->
            result[i].moves.forEach { slot ->
                if (!slot.isEmpty) assertTrue(slot.id in allIds)
            }
        }
    }

    @Test fun `story players are randomized when storyMoveProtection=false`() {
        val stats   = List(10) { makeStat(5, 15) }
        val players = List(10) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(storyMoveProtection = false), version)
            .randomize(stats, players, allMoves)
        val allIds = allMoves.map { it.id }.toSet()
        // all players (including story 0–5) should have pool moves
        result.forEach { stat ->
            stat.moves.filter { !it.isEmpty }.forEach { slot ->
                assertTrue(slot.id in allIds)
            }
        }
    }

    // ── samePositionMove ──────────────────────────────────────────────────────

    @Test fun `GK players only get GK moves when samePositionMove=true`() {
        val gkMoves = allMoves.filter { it.isKeeperMove }.map { it.id }.toSet()
        val stats   = listOf(makeStat(10, 20))
        val players = listOf(makeGkPlayer())
        val result  = MoveRandomizer(config(samePositionMove = true), version)
            .randomize(stats, players, allMoves)
        result[0].moves.filter { !it.isEmpty }.forEach { slot ->
            assertTrue(slot.id in gkMoves, "GK got non-GK move id=${slot.id}")
        }
    }

    @Test fun `outfield players get non-GK moves when samePositionMove=true`() {
        val nonGkMoves = allMoves.filter { !it.isKeeperMove }.map { it.id }.toSet()
        val stats   = listOf(makeStat(10, 20))
        val players = listOf(makeOutfieldPlayer())
        val result  = MoveRandomizer(config(samePositionMove = true), version)
            .randomize(stats, players, allMoves)
        result[0].moves.filter { !it.isEmpty }.forEach { slot ->
            assertTrue(slot.id in nonGkMoves, "outfield player got GK move id=${slot.id}")
        }
    }

    // ── IE1/IE2 move filters ──────────────────────────────────────────────────

    // IE1/IE2 filters only work for JAP IE2 ROMs; for EUR the distinction is meaningless.
    // These tests inject a known ie1JapMoveIds set so behavior is deterministic.
    private val japIe2Version = GameVersion.IE2_JA_FIRESTORM
    private val knownIe1Ids   = setOf(30, 50)  // ie1Move and ie1Move2 from allMoves

    @Test fun `onlyIe1Moves restricts pool to ie1 set for JAP IE2 ROM`() {
        val stats   = List(5) { makeStat(10, 20) }
        val players = List(5) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(onlyIe1Moves = true), japIe2Version, ie1JapMoveIds = knownIe1Ids)
            .randomize(stats, players, allMoves)
        result.forEach { stat ->
            stat.moves.filter { !it.isEmpty }.forEach { slot ->
                assertTrue(slot.id in knownIe1Ids, "expected IE1 move (${knownIe1Ids}), got id=${slot.id}")
            }
        }
    }

    @Test fun `onlyIe2Moves restricts pool to moves not in ie1 set for JAP IE2 ROM`() {
        val ie2Ids = allMoves.map { it.id }.toSet() - knownIe1Ids
        val stats   = List(5) { makeStat(10, 20) }
        val players = List(5) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(onlyIe2Moves = true), japIe2Version, ie1JapMoveIds = knownIe1Ids)
            .randomize(stats, players, allMoves)
        result.forEach { stat ->
            stat.moves.filter { !it.isEmpty }.forEach { slot ->
                assertTrue(slot.id in ie2Ids, "expected IE2-only move, got id=${slot.id}")
            }
        }
    }

    @Test fun `both ie1 and ie2 flags treats as no filter`() {
        val allIds = allMoves.map { it.id }.toSet()
        val stats   = List(20) { makeStat(10, 20) }
        val players = List(20) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(onlyIe1Moves = true, onlyIe2Moves = true), version)
            .randomize(stats, players, allMoves)
        result.forEach { stat ->
            stat.moves.filter { !it.isEmpty }.forEach { slot ->
                assertTrue(slot.id in allIds)
            }
        }
    }

    // ── limitOfSkill ──────────────────────────────────────────────────────────

    @Test fun `limitOfSkill filters moves above maxMoveLimit`() {
        val weakMoves   = listOf(makeMove(1, limit = 50), makeMove(2, limit = 100))
        val strongMoves = listOf(makeMove(3, limit = 200), makeMove(4, limit = 999))
        val pool        = weakMoves + strongMoves
        val weakIds     = weakMoves.map { it.id }.toSet()

        val stats   = List(5) { makeStat(10, 20) }
        val players = List(5) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(limitOfSkill = true, maxMoveLimit = 100), version)
            .randomize(stats, players, pool)
        result.forEach { stat ->
            stat.moves.filter { !it.isEmpty }.forEach { slot ->
                assertTrue(slot.id in weakIds, "expected weak move (limit≤100), got id=${slot.id}")
            }
        }
    }

    // ── randomMoveLevel ───────────────────────────────────────────────────────

    @Test fun `randomMoveLevel keeps level within 1 to maxMoveLevel`() {
        val max   = 50
        val stats   = List(20) { makeStat(10, 20, 30, 40) }
        val players = List(20) { makeOutfieldPlayer() }
        val result  = MoveRandomizer(config(randomMoveLevel = true, maxMoveLevel = max), version)
            .randomize(stats, players, allMoves)
        result.forEach { stat ->
            stat.moves.filter { !it.isEmpty }.forEach { slot ->
                assertTrue(slot.unlockLevel in 1..max, "level ${slot.unlockLevel} outside 1..$max")
            }
        }
    }

    @Test fun `when randomMoveLevel=false original levels are preserved`() {
        val stat    = makeStat(10, 20).let { s ->
            s.copy(moves = listOf(MoveSlot(10, 5), MoveSlot(20, 15), MoveSlot(0, 0), MoveSlot(0, 0)))
        }
        val players = listOf(makeOutfieldPlayer())
        val result  = MoveRandomizer(config(randomMoveLevel = false), version)
            .randomize(listOf(stat), players, allMoves)
        assertEquals(5,  result[0].moves[0].unlockLevel)
        assertEquals(15, result[0].moves[1].unlockLevel)
    }
}
