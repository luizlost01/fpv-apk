package com.fpvmirror

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView

/**
 * Custom TextureView for rendering stereo VR screen mirror or 2D mirror
 * Optimized for Phase 1 JPEG streaming
 */
class StereoSurfaceView(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs), TextureView.SurfaceTextureListener {

    private var isConnected = true
    private var isVRMode = true
    
    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    private var touchListener: ((x: Int, y: Int) -> Unit)? = null
    private var onStatusChanged: ((String) -> Unit)? = null

    init {
        surfaceTextureListener = this
        background = null
    }

    /**
     * Set rendering mode: true for SBS Stereo, false for 1:1 Mirror
     */
    fun setMode(vrMode: Boolean) {
        this.isVRMode = vrMode
        Log.d(TAG, "Mode set to: ${if (vrMode) "VR (SBS)" else "Mirror (2D)"}")
    }

    /**
     * Decode and display JPEG frame
     */
    fun decodeFrame(data: ByteArray) {
        try {
            // 1. Decode JPEG to Bitmap
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode JPEG data")
                return
            }

            // 2. Draw to Canvas
            val canvas = lockCanvas()
            if (canvas != null) {
                try {
                    renderFrame(canvas, bitmap)
                } finally {
                    unlockCanvasAndPost(canvas)
                }
            }
            
            // Cleanup bitmap quickly
            // bitmap.recycle() // Better let GC handle it if we are fast
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering frame: ${e.message}")
        }
    }

    private fun renderFrame(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawColor(Color.BLACK)

        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        
        if (isVRMode) {
            // Side-by-Side (SBS) mode: Duplicate image for both eyes
            val halfWidth = width / 2
            
            // Left eye
            val leftDest = Rect(0, 0, halfWidth, height)
            canvas.drawBitmap(bitmap, srcRect, leftDest, paint)
            
            // Right eye
            val rightDest = Rect(halfWidth, 0, width, height)
            canvas.drawBitmap(bitmap, srcRect, rightDest, paint)
            
            // Divider line
            paint.color = Color.DKGRAY
            canvas.drawLine(halfWidth.toFloat(), 0f, halfWidth.toFloat(), height.toFloat(), paint)
        } else {
            // Standard Mirror mode
            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bitmap, srcRect, destRect, paint)
        }
    }

    fun setConnected(connected: Boolean) {
        isConnected = connected
        if (connected) {
            onStatusChanged?.invoke("Connected - Receiving JPEG stream")
        } else {
            onStatusChanged?.invoke("Disconnected")
        }
    }

    fun setTouchListener(listener: (x: Int, y: Int) -> Unit) {
        touchListener = listener
    }

    fun setStatusChangeListener(listener: (String) -> Unit) {
        onStatusChanged = listener
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "SurfaceTexture available: ${width}x$height")
        onStatusChanged?.invoke("Surface Ready")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x.toInt()
                val touchY = event.y.toInt()

                val mappedX = if (isVRMode) {
                    if (touchX > width / 2) {
                        touchX - (width / 2)
                    } else {
                        touchX
                    }
                } else {
                    touchX
                }

                touchListener?.invoke(mappedX, touchY)
            }
        }
        return true
    }

    fun cleanup() {}

    companion object {
        private const val TAG = "StereoSurfaceView"
    }
}
