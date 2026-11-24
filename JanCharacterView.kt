package com.talkingjan.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class JanCharacterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Reaction { IDLE, LISTENING, SPEAKING, HAPPY, DIZZY, LAUGH, DRINK, EAT, DANCE }

    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.jan_character)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 68f
        textAlign = Paint.Align.CENTER
    }
    private val mouthGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 20, 184, 166)
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private var reaction = Reaction.IDLE
    private var reactionEndTime = 0L
    private var voiceLevel = 0f
    private var startTime = System.currentTimeMillis()

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var alpha: Int,
        val emoji: String
    )
    private val particles = mutableListOf<Particle>()

    var onInteraction: ((String) -> Unit)? = null

    fun setVoiceState(state: VoiceEngine.State, level: Float) {
        voiceLevel = level
        val now = System.currentTimeMillis()
        if (now > reactionEndTime || reaction == Reaction.IDLE || reaction == Reaction.LISTENING || reaction == Reaction.SPEAKING) {
            reaction = when (state) {
                VoiceEngine.State.IDLE -> Reaction.IDLE
                VoiceEngine.State.LISTENING -> Reaction.LISTENING
                VoiceEngine.State.SPEAKING -> Reaction.SPEAKING
            }
        }
        postInvalidateOnAnimation()
    }

    fun triggerReaction(type: Reaction, durationMs: Long, emoji: String) {
        reaction = type
        reactionEndTime = System.currentTimeMillis() + durationMs
        spawnParticles(width * 0.5f, height * 0.35f, emoji, 10)
        postInvalidateOnAnimation()
    }

    private fun spawnParticles(cx: Float, cy: Float, emoji: String, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = cx + Random.nextFloat() * 240f - 120f,
                    y = cy + Random.nextFloat() * 140f - 70f,
                    vx = Random.nextFloat() * 6f - 3f,
                    vy = -Random.nextFloat() * 7f - 3f,
                    alpha = 255,
                    emoji = emoji
                )
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val relY = event.y / height.coerceAtLeast(1)
            when {
                relY < 0.28f -> {
                    triggerReaction(Reaction.HAPPY, 1400, "❤️")
                    SoundEffects.playPurr()
                    onInteraction?.invoke("Head pat! Jan loves it ❤️")
                }
                relY < 0.52f -> {
                    triggerReaction(Reaction.DIZZY, 1200, "⭐")
                    SoundEffects.playBoing()
                    onInteraction?.invoke("Hey! Don't poke Jan's face! ⭐")
                }
                else -> {
                    triggerReaction(Reaction.LAUGH, 1300, "😂")
                    SoundEffects.playGiggle()
                    onInteraction?.invoke("Hehe! That tickles! 😂")
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val t = (now - startTime) / 1000f

        if (reaction != Reaction.IDLE && reaction != Reaction.LISTENING && reaction != Reaction.SPEAKING && now > reactionEndTime) {
            reaction = Reaction.IDLE
        }

        var scaleX = 1.0f
        var scaleY = 1.0f + sin(t * 2.2f) * 0.012f
        var rotDeg = 0f
        var transX = 0f
        var transY = 0f

        when (reaction) {
            Reaction.LISTENING -> {
                rotDeg = 4.5f
                scaleX = 1.03f
                scaleY = 1.03f
            }
            Reaction.SPEAKING -> {
                val bounce = voiceLevel * 0.09f
                scaleX = 1.0f + bounce * 0.6f
                scaleY = 1.0f - bounce * 0.35f
                transY = -voiceLevel * 18f
                rotDeg = sin(t * 24f) * (2.5f + voiceLevel * 4f)
            }
            Reaction.HAPPY -> {
                rotDeg = sin(t * 10f) * 5f
                scaleY = 1.04f
            }
            Reaction.DIZZY -> {
                transX = sin(t * 45f) * 22f
                rotDeg = sin(t * 28f) * 9f
            }
            Reaction.LAUGH -> {
                transY = -absSin(t * 18f) * 24f
                scaleX = 1.05f
                scaleY = 0.96f
            }
            Reaction.DRINK -> {
                rotDeg = -8f
                transY = sin(t * 12f) * 10f
            }
            Reaction.EAT -> {
                scaleY = 1.0f + sin(t * 22f) * 0.05f
            }
            Reaction.DANCE -> {
                rotDeg = sin(t * 12f) * 10f
                transX = cos(t * 6f) * 28f
                transY = -absSin(t * 12f) * 22f
            }
            Reaction.IDLE -> {}
        }

        canvas.save()
        canvas.translate(width / 2f + transX, height / 2f + transY)
        canvas.rotate(rotDeg)
        canvas.scale(scaleX, scaleY)

        val scale = maxOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val dst = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
        canvas.drawBitmap(bitmap, null, dst, paint)

        if (reaction == Reaction.SPEAKING || reaction == Reaction.LISTENING) {
            val radius = 140f + voiceLevel * 160f
            mouthGlowPaint.alpha = if (reaction == Reaction.SPEAKING) 140 else 80
            canvas.drawCircle(0f, -height * 0.05f, radius, mouthGlowPaint)
        }
        canvas.restore()

        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.alpha -= 6
            if (p.alpha <= 0) {
                iter.remove()
            } else {
                emojiPaint.alpha = p.alpha
                canvas.drawText(p.emoji, p.x, p.y, emojiPaint)
            }
        }

        postInvalidateOnAnimation()
    }

    private fun absSin(x: Float): Float {
        val s = sin(x)
        return if (s < 0) -s else s
    }
}
