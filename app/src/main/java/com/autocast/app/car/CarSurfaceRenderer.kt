package com.autocast.app.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.Surface

/**
 * Holds the drawable Surface that Android Auto gives navigation apps and
 * draws mirrored phone frames onto it. Thread-safe.
 */
object CarSurfaceRenderer {

    private val lock = Any()
    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }
    @Volatile var hasFrames = false

    fun setSurface(s: Surface?, w: Int, h: Int) {
        synchronized(lock) {
            surface = s
            surfaceWidth = w
            surfaceHeight = h
        }
        if (s != null) drawMessage("Open AutoCast on your phone and tap Start Mirroring")
    }

    fun updateSize(w: Int, h: Int) {
        synchronized(lock) {
            surfaceWidth = w
            surfaceHeight = h
        }
    }

    fun drawFrame(bitmap: Bitmap) {
        synchronized(lock) {
            val s = surface ?: return
            if (!s.isValid) return
            val canvas: Canvas = try { s.lockCanvas(null) } catch (e: Exception) { return }
            try {
                canvas.drawColor(Color.BLACK)
                val cw = canvas.width.toFloat()
                val ch = canvas.height.toFloat()
                val scale = minOf(cw / bitmap.width, ch / bitmap.height)
                val dw = (bitmap.width * scale).toInt()
                val dh = (bitmap.height * scale).toInt()
                val left = ((cw - dw) / 2).toInt()
                val top = ((ch - dh) / 2).toInt()
                canvas.drawBitmap(
                    bitmap,
                    Rect(0, 0, bitmap.width, bitmap.height),
                    Rect(left, top, left + dw, top + dh),
                    paint
                )
                hasFrames = true
            } finally {
                try { s.unlockCanvasAndPost(canvas) } catch (e: Exception) { }
            }
        }
    }

    fun drawMessage(msg: String) {
        synchronized(lock) {
            val s = surface ?: return
            if (!s.isValid) return
            val canvas = try { s.lockCanvas(null) } catch (e: Exception) { return }
            try {
                canvas.drawColor(Color.BLACK)
                canvas.drawText(msg, canvas.width / 2f, canvas.height / 2f, textPaint)
            } finally {
                try { s.unlockCanvasAndPost(canvas) } catch (e: Exception) { }
            }
        }
    }
}
