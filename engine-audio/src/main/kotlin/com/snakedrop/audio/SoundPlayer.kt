package com.snakedrop.audio

/** Sound effects the game can request. Mapped from engine events by the host. */
enum class Sfx { APPLE, DETACH, LAND, ROWS, WARNING, GAME_OVER }

/** Audio sink. Real playback (SoundPool) is added once sound assets exist; until then use [NoOpSoundPlayer]. */
interface SoundPlayer {
    fun play(sfx: Sfx)
}

/** Silent implementation used until assets are added and in tests. */
object NoOpSoundPlayer : SoundPlayer {
    override fun play(sfx: Sfx) = Unit
}
