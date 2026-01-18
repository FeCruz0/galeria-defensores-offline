package com.galeria.defensores.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.galeria.defensores.R
import kotlin.math.abs
import kotlin.random.Random

class DiceBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Die(
        var x: Float,
        var y: Float,
        var dx: Float = 0f,
        var dy: Float = 0f,
        var color: Int = Color.WHITE,
        var value: Int = 1,
        var rotation: Float = 0f,
        var dhRotation: Float = 0f, // rotational velocity
        var state: DieState = DieState.IDLE,
        val size: Float = 150f,
        var canCrit: Boolean = false,
        var isNegative: Boolean = false,
        var critRangeStart: Int = 6
    )

    enum class DieState {
        IDLE, DRAGGING, ROLLING, SETTLED
    }

    private val dice = mutableListOf<Die>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var velocityTracker: VelocityTracker? = null
    var onRollFinished: ((List<Int>) -> Unit)? = null
    
    private var activeDieIndex: Int = -1
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    
    // For synchronization: if set, dice will land on these values
    var expectedResults: List<Int>? = null

    // Sounds
    private var soundPool: SoundPool? = null
    private var shakeSoundId: Int = 0
    private var throwSoundId: Int = 0
    private var lastShakeTime: Long = 0
    private val SHAKE_COOLDOWN = 150L // ms
    private val loadedSounds = mutableSetOf<Int>()

    // Vibration
    private var vibrator: Vibrator? = null

    init {
        pipPaint.color = Color.BLACK
        pipPaint.style = Paint.Style.FILL
        
        shadowPaint.color = Color.parseColor("#40000000")
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)

        glowPaint.color = Color.parseColor("#FFD700") // Golden
        glowPaint.style = Paint.Style.FILL
        glowPaint.maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
        
        initSoundPool()
        initVibrator()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun initSoundPool() {
        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attr)
            .build()
        
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
                android.util.Log.d("DiceBoardSound", "Sound loaded: $sampleId")
            } else {
                android.util.Log.e("DiceBoardSound", "Error loading sound $sampleId: $status")
            }
        }
        
        shakeSoundId = soundPool?.load(context, R.raw.dice_shake, 1) ?: 0
        throwSoundId = soundPool?.load(context, R.raw.dice_throw, 1) ?: 0
        android.util.Log.d("DiceBoardSound", "Loading sounds: shake=$shakeSoundId, throw=$throwSoundId")
    }

    private fun playShakeSound() {
        if (!loadedSounds.contains(shakeSoundId)) {
            android.util.Log.w("DiceBoardSound", "Shake sound not loaded yet")
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastShakeTime > SHAKE_COOLDOWN) {
            soundPool?.play(shakeSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            lastShakeTime = now
        }
    }

    private fun playThrowSound() {
        if (!loadedSounds.contains(throwSoundId)) {
            android.util.Log.w("DiceBoardSound", "Throw sound not loaded yet")
            return
        }
        soundPool?.play(throwSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun initializeDice(count: Int, canCrit: Boolean = false, isNegative: Boolean = false, critRangeStart: Int = 6, diceProperties: List<com.galeria.defensores.models.DieProperty>? = null) {
        dice.clear()
        // Spawn dice in center or bottom area
        val startX = width / 2f
        val startY = height - 400f
        
        for (i in 0 until count) {
            val prop = diceProperties?.getOrNull(i)
            
            val dieCanCrit = prop?.canCrit ?: canCrit
            val dieIsNegative = prop?.isNegative ?: isNegative
            val dieCritRange = prop?.critRangeStart ?: critRangeStart
            
            val dieColor = when {
                dieIsNegative -> Color.BLACK
                !dieCanCrit -> Color.RED
                else -> Color.WHITE
            }

            dice.add(Die(
                x = startX + (i * 180f) - ((count - 1) * 90f) - 75f, // Centered spread
                y = startY,
                rotation = Random.nextFloat() * 360f,
                color = dieColor,
                canCrit = dieCanCrit,
                isNegative = dieIsNegative,
                critRangeStart = dieCritRange
            ))
        }
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var allSettled = true
        var moving = false

        dice.forEach { die ->
            // Physics Update
            if (die.state == DieState.ROLLING) {
                moving = true
                allSettled = false
                
                die.x += die.dx
                die.y += die.dy
                die.rotation += die.dhRotation
                
                // Bounce
                if (die.x < 0) { 
                    die.x = 0f; die.dx *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 
                    vibrate(10)
                }
                if (die.x + die.size > width) { 
                    die.x = width - die.size; die.dx *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 
                    vibrate(10)
                }
                if (die.y < 0) { 
                    die.y = 0f; die.dy *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 
                    vibrate(10)
                }
                if (die.y + die.size > height) { 
                    die.y = height - die.size; die.dy *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 
                    vibrate(10)
                }
                
                // Friction
                die.dx *= 0.98f
                die.dy *= 0.98f
                die.dhRotation *= 0.97f
                
                // Spin/Value change while rolling fast
                if (abs(die.dx) + abs(die.dy) > 10) {
                   if (Random.nextInt(5) == 0) die.value = Random.nextInt(6) + 1
                } else {
                    // Snap rotation when slowing down to look neat-ish? 
                    // Or usually dice just stop.
                }

                // Stop condition
                if (abs(die.dx) < 1.5f && abs(die.dy) < 1.5f && abs(die.dhRotation) < 1.5f) {
                    die.state = DieState.SETTLED
                    // Use expected result if available
                    val idx = dice.indexOf(die)
                    die.value = if (expectedResults != null && idx < expectedResults!!.size) {
                        expectedResults!![idx]
                    } else {
                        Random.nextInt(6) + 1
                    }
                }
            } else if (die.state == DieState.DRAGGING) {
                allSettled = false
                // Wiggle while dragging?
                die.dx = (die.x - lastTouchX) * 0.1f // Fake momentum for visual
            } else if (die.state == DieState.IDLE) {
                 allSettled = false // Must interact
            }

            // Dice-to-Dice Collision Detection
            for (j in (dice.indexOf(die) + 1) until dice.size) {
                val other = dice[j]
                if (die.state != DieState.ROLLING && other.state != DieState.ROLLING) continue

                val dxCollision = (other.x + other.size / 2) - (die.x + die.size / 2)
                val dyCollision = (other.y + other.size / 2) - (die.y + die.size / 2)
                val distance = Math.sqrt((dxCollision * dxCollision + dyCollision * dyCollision).toDouble()).toFloat()
                val minDistance = (die.size + other.size) / 2.2f // Slightly smaller than full size for better feel

                if (distance < minDistance && distance > 0) {
                    // Static Resolution (Push apart)
                    val overlap = minDistance - distance
                    val nx = dxCollision / distance // Normal X
                    val ny = dyCollision / distance // Normal Y
                    
                    val moveX = nx * overlap / 2
                    val moveY = ny * overlap / 2
                    
                    if (die.state == DieState.ROLLING) {
                        die.x -= moveX
                        die.y -= moveY
                    }
                    if (other.state == DieState.ROLLING) {
                        other.x += moveX
                        other.y += moveY
                    }

                    // Dynamic Resolution (Elastic Collision)
                    // Relative velocity
                    val rvx = other.dx - die.dx
                    val rvy = other.dy - die.dy
                    
                    // Relative velocity along normal
                    val velAlongNormal = rvx * nx + rvy * ny
                    
                    // Do not resolve if velocities are separating
                    if (velAlongNormal < 0) {
                        // Restitution (bounciness)
                        val e = 0.6f
                        
                        // Impulse scalar
                        var jImpulse = -(1 + e) * velAlongNormal
                        jImpulse /= 2 // Assuming equal mass (1)
                        
                        // Apply impulse
                        val impulseX = jImpulse * nx
                        val impulseY = jImpulse * ny
                        
                        if (die.state == DieState.ROLLING) {
                            die.dx -= impulseX
                            die.dy -= impulseY
                            die.dhRotation += (Random.nextFloat() * 20 - 10)
                        }
                        if (other.state == DieState.ROLLING) {
                            other.dx += impulseX
                            other.dy += impulseY
                            other.dhRotation += (Random.nextFloat() * 20 - 10)
                        }
                        
                        // Play sound if impact is hard enough
                        if (Math.abs(velAlongNormal) > 5) {
                            playShakeSound() // Using shake sound as collision placeholder
                            vibrate(15)
                        }
                    }
                }
            }

            // Draw Shadow
            val shadowRect = RectF(die.x + 10, die.y + 10, die.x + die.size + 10, die.y + die.size + 10)
            canvas.drawOval(shadowRect, shadowPaint)

            // Draw Glow for Crits
            if (die.canCrit && !die.isNegative && die.value >= die.critRangeStart && die.state == DieState.SETTLED) {
                canvas.drawCircle(die.x + die.size/2, die.y + die.size/2, die.size * 0.8f, glowPaint)
            }
            
            // Draw Die
            canvas.save()
            canvas.rotate(die.rotation, die.x + die.size/2, die.y + die.size/2)
            
            val rect = RectF(die.x, die.y, die.x + die.size, die.y + die.size)
            
            // 3D-ish Gradient
            val surfaceColor = die.color
            val darkerColor = manipulateColor(surfaceColor, 0.7f)
            val highlightColor = if (die.isNegative) Color.parseColor("#444444") else Color.WHITE

            val gradient = RadialGradient(
                die.x + die.size*0.3f, 
                die.y + die.size*0.3f, 
                die.size, 
                intArrayOf(highlightColor, surfaceColor, darkerColor), 
                floatArrayOf(0f, 0.5f, 1f), 
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            paint.style = Paint.Style.FILL
            paint.color = die.color // Use die's specific color
            canvas.drawRoundRect(rect, 30f, 30f, paint)
            
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.color = if (die.isNegative) Color.GRAY else Color.DKGRAY
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rect, 30f, 30f, paint)
            
            // Draw Pips
            pipPaint.color = if (die.color == Color.WHITE) Color.BLACK else Color.WHITE
            drawPips(canvas, die.value, rect)
            
            canvas.restore()
        }
        
        if (moving) {
            invalidate()
        } else if (allSettled && dice.isNotEmpty()) {
            val hasJustSettled = dice.all { it.state == DieState.SETTLED }
            if (hasJustSettled) {
                val results = dice.map { it.value }
                onRollFinished?.invoke(results)
            }
        }
    }
    
    private fun drawPips(canvas: Canvas, value: Int, rect: RectF) {
        val pipSize = rect.width() / 10f
        val cx = rect.centerX()
        val cy = rect.centerY()
        val l = rect.left + rect.width() * 0.25f
        val r = rect.left + rect.width() * 0.75f
        val t = rect.top + rect.height() * 0.25f
        val b = rect.top + rect.height() * 0.75f
        
        when (value) {
            1 -> drawDot(canvas, cx, cy, pipSize)
            2 -> { drawDot(canvas, l, t, pipSize); drawDot(canvas, r, b, pipSize) }
            3 -> { drawDot(canvas, l, t, pipSize); drawDot(canvas, cx, cy, pipSize); drawDot(canvas, r, b, pipSize) }
            4 -> { drawDot(canvas, l, t, pipSize); drawDot(canvas, r, t, pipSize); drawDot(canvas, l, b, pipSize); drawDot(canvas, r, b, pipSize) }
            5 -> { drawDot(canvas, l, t, pipSize); drawDot(canvas, r, t, pipSize); drawDot(canvas, cx, cy, pipSize); drawDot(canvas, l, b, pipSize); drawDot(canvas, r, b, pipSize) }
            6 -> { 
                drawDot(canvas, l, t, pipSize); drawDot(canvas, r, t, pipSize)
                drawDot(canvas, l, cy, pipSize); drawDot(canvas, r, cy, pipSize)
                drawDot(canvas, l, b, pipSize); drawDot(canvas, r, b, pipSize)
            }
        }
    }
    
    private fun drawDot(canvas: Canvas, x: Float, y: Float, radius: Float) {
        canvas.drawCircle(x, y, radius, pipPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeDieIndex = dice.indexOfLast { 
                    x >= it.x && x <= it.x + it.size && y >= it.y && y <= it.y + it.size && it.state == DieState.IDLE
                }
                if (activeDieIndex != -1) {
                    dice[activeDieIndex].state = DieState.DRAGGING
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeDieIndex != -1) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    val die = dice[activeDieIndex]
                    die.x += dx
                    die.y += dy
                    lastTouchX = x
                    lastTouchY = y
                    playShakeSound()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeDieIndex != -1) {
                    val die = dice[activeDieIndex]
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f
                    
                    // ALWAYS ROLL
                    die.state = DieState.ROLLING
                    die.dx = vx * 0.015f 
                    die.dy = vy * 0.015f
                    // Add slight rotation if none exists to ensure motion check passes
                    val randomSpin = (Random.nextFloat() * 20f) - 10f
                    die.dhRotation = randomSpin
                    
                    // If velocity is extremely low, ensure at least some motion so it doesn't settle instantly in a weird state
                    // logic handles checking dx < 2. If 0, it settles immediately effectively.
                    
                    activeDieIndex = -1
                    playThrowSound()
                    vibrate(30)
                    invalidate()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Programmatically triggers a roll animation for observers.
     */
    fun autoFlick() {
        dice.forEach { die ->
                die.state = DieState.ROLLING
                // Randomized flick - Increased for stronger shake/flick feel
                die.dx = (Random.nextFloat() * 120f - 60f)
                die.dy = (Random.nextFloat() * 120f - 60f)
                die.dhRotation = (Random.nextFloat() * 100f - 50f)
        }
        playThrowSound()
        vibrate(30)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundPool?.release()
        soundPool = null
    }

    private fun manipulateColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Math.round(Color.red(color) * factor)
        val g = Math.round(Color.green(color) * factor)
        val b = Math.round(Color.blue(color) * factor)
        return Color.argb(a,
            Math.min(r.toInt(), 255),
            Math.min(g.toInt(), 255),
            Math.min(b.toInt(), 255)
        )
    }
}
