@file:Suppress("MagicNumber")

package com.snakedrop.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb

/** Fixed-size square-pixel particle pool. No allocation after construction; oldest slots are reused when full. */
class ParticleSystem(private val capacity: Int = 256) {
    private val x = FloatArray(capacity)
    private val y = FloatArray(capacity)
    private val vx = FloatArray(capacity)
    private val vy = FloatArray(capacity)
    private val life = FloatArray(capacity)
    private val maxLife = FloatArray(capacity)
    private val size = FloatArray(capacity)
    private val argb = IntArray(capacity)
    private var next = 0
    private var seed = 0x2F6E2B1

    private fun rnd(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 8) and 0xFFFF) / 65535f
    }

    /** Emits [count] particles from ([cx], [cy]) in all directions at up to [speed] px/s, living about [lifeSec]. */
    fun burst(
        cx: Float,
        cy: Float,
        count: Int,
        color: Color,
        speed: Float,
        sizePx: Float,
        lifeSec: Float = 0.6f,
    ) {
        val c = color.toArgb()
        repeat(count) {
            val i = next
            next = (next + 1) % capacity
            val a = rnd() * 6.2831855f
            val s = speed * (0.3f + 0.7f * rnd())
            x[i] = cx
            y[i] = cy
            vx[i] = kotlin.math.cos(a) * s
            vy[i] = kotlin.math.sin(a) * s - speed * 0.25f
            maxLife[i] = lifeSec * (0.6f + 0.6f * rnd())
            life[i] = maxLife[i]
            size[i] = sizePx * (0.6f + 0.8f * rnd())
            argb[i] = c
        }
    }

    /** Advances all live particles by [dtSec]. */
    fun update(dtSec: Float) {
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            life[i] -= dtSec
            vy[i] += GRAVITY * dtSec
            x[i] += vx[i] * dtSec
            y[i] += vy[i] * dtSec
        }
    }

    /** Draws live particles, fading out over their last half-life. */
    fun draw(scope: DrawScope) {
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            val a = (life[i] / maxLife[i] * 2f).coerceIn(0f, 1f)
            scope.drawRect(Color(argb[i]).copy(alpha = a), Offset(x[i], y[i]), Size(size[i], size[i]))
        }
    }

    private companion object {
        const val GRAVITY = 900f
    }
}
