package com.example.semimanufactures

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var matrix: Matrix = Matrix()
    private var scaleDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector

    // Переменные для масштабирования
    var scaleFactor = 1.0f
    private val minScale = 1.0f
    private val maxScale = 5.0f

    // Переменные для перемещения
    private var lastTouch = PointF()
    private var startTouch = PointF()

    // Режимы
    private var mode = Mode.NONE

    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    // Матричные значения
    private val matrixValues = FloatArray(9)

    // Флаг для отслеживания двойного тапа
    private var isDoubleTapping = false

    init {
        scaleType = ScaleType.MATRIX
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        // Включаем мультитач
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val currentTouch = PointF(event.x, event.y)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                startTouch.set(currentTouch)
                lastTouch.set(currentTouch)
                mode = Mode.DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                lastTouch.set(currentTouch)
                startTouch.set(currentTouch)
                mode = Mode.ZOOM
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDoubleTapping) {
                    if (mode == Mode.DRAG) {
                        val deltaX = currentTouch.x - lastTouch.x
                        val deltaY = currentTouch.y - lastTouch.y

                        // Ограничиваем перемещение
                        if (scaleFactor > minScale) {
                            val fixTrans = getFixedTranslation(deltaX, deltaY)
                            matrix.postTranslate(fixTrans.x, fixTrans.y)
                            fixTrans()
                            imageMatrix = matrix
                        }
                        lastTouch.set(currentTouch.x, currentTouch.y)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
                isDoubleTapping = false
            }
        }

        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScale = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)

            val focusX = detector.focusX
            val focusY = detector.focusY

            matrix.postScale(scaleFactor / oldScale, scaleFactor / oldScale, focusX, focusY)
            fixTrans() // Ограничиваем позицию после масштабирования
            imageMatrix = matrix

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = Mode.ZOOM
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            isDoubleTapping = true
            if (scaleFactor > minScale + 0.1f) {
                // Если увеличен - сбрасываем
                resetZoom()
            } else {
                // Если не увеличен - увеличиваем в 2 раза
                scaleFactor = 2.0f
                matrix.setScale(scaleFactor, scaleFactor, e.x, e.y)
                fixTrans() // Ограничиваем позицию после масштабирования
                imageMatrix = matrix
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return performClick()
        }
    }

    private fun fixTrans() {
        matrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]

        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth * scaleFactor
        val imageHeight = drawable.intrinsicHeight * scaleFactor

        val fixTransX = getFixTrans(transX, width.toFloat(), imageWidth)
        val fixTransY = getFixTrans(transY, height.toFloat(), imageHeight)

        if (fixTransX != 0f || fixTransY != 0f) {
            matrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixedTranslation(deltaX: Float, deltaY: Float): PointF {
        matrix.getValues(matrixValues)
        val currentTransX = matrixValues[Matrix.MTRANS_X]
        val currentTransY = matrixValues[Matrix.MTRANS_Y]

        val drawable = drawable ?: return PointF(deltaX, deltaY)
        val imageWidth = drawable.intrinsicWidth * scaleFactor
        val imageHeight = drawable.intrinsicHeight * scaleFactor

        // Вычисляем допустимые границы перемещения
        val minTransX: Float
        val maxTransX: Float
        val minTransY: Float
        val maxTransY: Float

        if (imageWidth <= width) {
            // Изображение меньше или равно ширине экрана - центрируем
            minTransX = (width - imageWidth) / 2
            maxTransX = (width - imageWidth) / 2
        } else {
            // Изображение больше ширины экрана - можно перемещать в пределах
            minTransX = width - imageWidth
            maxTransX = 0f
        }

        if (imageHeight <= height) {
            // Изображение меньше или равно высоте экрана - центрируем
            minTransY = (height - imageHeight) / 2
            maxTransY = (height - imageHeight) / 2
        } else {
            // Изображение больше высоты экрана - можно перемещать в пределах
            minTransY = height - imageHeight
            maxTransY = 0f
        }

        // Вычисляем новую позицию
        val newTransX = currentTransX + deltaX
        val newTransY = currentTransY + deltaY

        // Ограничиваем перемещение по X
        val fixedDeltaX = when {
            newTransX > maxTransX -> maxTransX - currentTransX
            newTransX < minTransX -> minTransX - currentTransX
            else -> deltaX
        }

        // Ограничиваем перемещение по Y
        val fixedDeltaY = when {
            newTransY > maxTransY -> maxTransY - currentTransY
            newTransY < minTransY -> minTransY - currentTransY
            else -> deltaY
        }

        return PointF(fixedDeltaX, fixedDeltaY)
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            // Контент меньше вью - центрируем
            minTrans = (viewSize - contentSize) / 2
            maxTrans = (viewSize - contentSize) / 2
        } else {
            // Контент больше вью - ограничиваем краями
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        if (trans < minTrans) return minTrans - trans
        if (trans > maxTrans) return maxTrans - trans
        return 0f
    }

    fun resetZoom() {
        scaleFactor = 1.0f
        matrix.reset()
        centerImage()
    }

    private fun centerImage() {
        val drawable = drawable ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val scale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight)

        // Центрируем изображение
        val dx = (viewWidth - drawableWidth * scale) / 2
        val dy = (viewHeight - drawableHeight * scale) / 2

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        imageMatrix = matrix
        scaleFactor = scale
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerImage()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        post { centerImage() }
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        post { centerImage() }
    }
}