package com.snakedrop.platform

/** One row of a daily-race leaderboard. */
data class RaceEntry(
    val name: String,
    val score: Int,
    val isYou: Boolean,
)

/**
 * Daily seed race backend. A real implementation uploads [replay] (the server re-simulates it to verify the score)
 * and returns the day's leaderboard. [OfflineRaceBackend] is the stand-in until a server exists.
 */
interface RaceBackend {
    /** True when scores are shared with other real players. */
    val isOnline: Boolean

    suspend fun submit(
        day: Long,
        playerName: String,
        score: Int,
        replay: ByteArray,
    ): List<RaceEntry>

    suspend fun leaderboard(day: Long): List<RaceEntry>
}

/** Local stand-in: deterministic demo rivals per day plus this device's best score. Not real players. */
class OfflineRaceBackend : RaceBackend {
    override val isOnline: Boolean = false
    private val best = HashMap<Long, Int>()

    override suspend fun submit(
        day: Long,
        playerName: String,
        score: Int,
        replay: ByteArray,
    ): List<RaceEntry> {
        if (score > (best[day] ?: -1)) best[day] = score
        return leaderboard(day)
    }

    override suspend fun leaderboard(day: Long): List<RaceEntry> {
        val rivals = RIVALS.mapIndexed { i, name -> RaceEntry(name, rivalScore(day, i), false) }
        val mine = best[day]?.let { RaceEntry("YOU", it, true) }
        return (rivals + listOfNotNull(mine)).sortedByDescending { it.score }
    }

    private fun rivalScore(
        day: Long,
        index: Int,
    ): Int {
        val h = (day * MIX_A + index * MIX_B) xor (day shl SHIFT)
        return BASE + (h and MASK).toInt() % SPREAD
    }

    private companion object {
        val RIVALS = listOf("NOVA", "PIXEL", "RAVEN", "BYTE", "COMET", "ZIGZAG", "ORBIT", "MAMBA")
        const val MIX_A = 6364136223846793005L
        const val MIX_B = 1442695040888963407L
        const val SHIFT = 7
        const val MASK = 0x7FFFFFFFL
        const val BASE = 120
        const val SPREAD = 600
    }
}
