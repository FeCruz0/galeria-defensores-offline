package com.galeria.defensores.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
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
        var value: Int = 1,
        var rotation: Float = 0f,
        var dhRotation: Float = 0f, // rotational velocity
        var state: DieState = DieState.IDLE,
        val size: Float = 150f,
        val color: Int = Color.WHITE
    )

    enum class DieState {
        IDLE, DRAGGING, ROLLING, SETTLED
    }

    private val dice = mutableListOf<Die>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var velocityTracker: VelocityTracker? = null
    var onRollFinished: ((List<Int>) -> Unit)? = null
    
    private var activeDieIndex: Int = -1
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    init {
        pipPaint.color = Color.BLACK
        pipPaint.style = Paint.Style.FILL
        
        shadowPaint.color = Color.parseColor("#40000000")
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    fun initializeDice(count: Int) {
        dice.clear()
        // Spawn dice in center or bottom area
        val startX = width / 2f
        val startY = height - 400f
        
        for (i in 0 until count) {
            dice.add(Die(
                x = startX + (i * 180f) - ((count - 1) * 90f) - 75f, // Centered spread
                y = startY,
                rotation = Random.nextFloat() * 360f
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
                if (die.x < 0) { die.x = 0f; die.dx *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 }
                if (die.x + die.size > width) { die.x = width - die.size; die.dx *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 }
                if (die.y < 0) { die.y = 0f; die.dy *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 }
                if (die.y + die.size > height) { die.y = height - die.size; die.dy *= -0.7f; die.dhRotation += Random.nextFloat() * 20 - 10 }
                
                // Friction
                die.dx *= 0.96f
                die.dy *= 0.96f
                die.dhRotation *= 0.95f
                
                // Spin/Value change while rolling fast
                if (abs(die.dx) + abs(die.dy) > 10) {
                   if (Random.nextInt(5) == 0) die.value = Random.nextInt(6) + 1
                } else {
                    // Snap rotation when slowing down to look neat-ish? 
                    // Or usually dice just stop.
                }

                // Stop condition
                if (abs(die.dx) < 2f && abs(die.dy) < 2f && abs(die.dhRotation) < 2f) {
                    die.state = DieState.SETTLED
                    die.value = Random.nextInt(6) + 1 // Final value (or logic)
                }
            } else if (die.state == DieState.DRAGGING) {
                allSettled = false
                // Wiggle while dragging?
                die.dx = (die.x - lastTouchX) * 0.1f // Fake momentum for visual
            } else if (die.state == DieState.IDLE) {
                 allSettled = false // Must interact
            }
            // If SETTLED, allSettled remains true (unless another die sets it false)
            // If SETTLED, allSettled remains true (unless another die sets it false)

            // Draw Shadow
            val shadowRect = RectF(die.x + 10, die.y + 10, die.x + die.size + 10, die.y + die.size + 10)
            canvas.drawOval(shadowRect, shadowPaint)

            // Draw Die
            canvas.save()
            canvas.rotate(die.rotation, die.x + die.size/2, die.y + die.size/2)
            
            val rect = RectF(die.x, die.y, die.x + die.size, die.y + die.size)
            
            // 3D-ish Gradient
            val gradient = RadialGradient(
                die.x + die.size*0.3f, 
                die.y + die.size*0.3f, 
                die.size, 
                intArrayOf(Color.WHITE, Color.LTGRAY, Color.GRAY), 
                floatArrayOf(0f, 0.8f, 1f), 
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 30f, 30f, paint)
            
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.color = Color.DKGRAY
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rect, 30f, 30f, paint)
            
            // Draw Pips
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
                dice.clear() 
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
                    die.dx = vx * 0.03f 
                    die.dy = vy * 0.03f
                    // Add slight rotation if none exists to ensure motion check passes
                    val randomSpin = (Random.nextFloat() * 30f) - 15f
                    die.dhRotation = randomSpin
                    
                    // If velocity is extremely low, ensure at least some motion so it doesn't settle instantly in a weird state
                    // logic handles checking dx < 2. If 0, it settles immediately effectively.
                    
                    activeDieIndex = -1
                    invalidate()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
