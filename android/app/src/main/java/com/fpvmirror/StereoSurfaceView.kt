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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Optimized StereoSurfaceView for low-latency JPEG streaming.
 * Uses a dedicated background thread for decoding and drops stale frames.
 */
class StereoSurfaceView(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs), TextureView.SurfaceTextureListener {

    private var isVRMode = true
    private val isProcessing = AtomicBoolean(false)
    private val renderExecutor = Executors.newSingleThreadExecutor()
    
    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = false // Faster rendering
    }

    private var touchListener: ((x: Int, y: Int) -> Unit)? = null
    private var onStatusChanged: ((String) -> Unit)? = null

    // Pre-allocate Rects to avoid GC pressure
    private val srcRect = Rect()
    private val leftDest = Rect()
    private val rightDest = Rect()
    private val fullDest = Rect()

    init {
        surfaceTextureListener = this
        background = null
    }

    fun setMode(vrMode: Boolean) {
        this.isVRMode = vrMode
    }

    /**
     * Non-blocking frame handler. If a frame is already being processed, 
     * this will drop the incoming frame to prevent buffer bloat.
     */
    fun decodeFrame(data: ByteArray) {
        // DROP FRAME if we are still busy processing the previous one
        if (isProcessing.compareAndSet(false, true)) {
            renderExecutor.execute {
                try {
                    val options = BitmapFactory.Options().apply {
                        inMutable = true
                        inPreferredConfig = Bitmap.Config.RGB_565 // Less memory, faster
                    }
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                    
                    if (bitmap != null) {
                        val canvas = lockCanvas()
                        if (canvas != null) {
                            try {
                                renderToCanvas(canvas, bitmap)
                            } finally {
                                unlockCanvasAndPost(canvas)
                            }
                        }
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Render error: ${e.message}")
                } finally {
                    isProcessing.set(false)
                }
            }
        }
    }

    private fun renderToCanvas(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawColor(Color.BLACK)
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        
        if (isVRMode) {
            val halfWidth = width / 2
            leftDest.set(0, 0, halfWidth, height)
            canvas.drawBitmap(bitmap, srcRect, leftDest, paint)
            
            rightDest.set(halfWidth, 0, width, height)
            canvas.drawBitmap(bitmap, srcRect, rightDest, paint)
        } else {
            fullDest.set(0, 0, width, height)
            canvas.drawBitmap(bitmap, srcRect, fullDest, paint)
        }
    }

    fun setConnected(connected: Boolean) {
        if (connected) {
            onStatusChanged?.invoke("Live Stream - Low Latency Mode")
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

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderExecutor.shutdownNow()
        return true
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val x = event.x.toInt()
            val mappedX = if (isVRMode) if (x > width / 2) x - (width / 2) else x else x
            touchListener?.invoke(mappedX, event.y.toInt())
        }
        return true
    }

    fun cleanup() {
        renderExecutor.shutdownNow()
    }

    companion object {
        private const val TAG = "StereoSurfaceView"
    }
}
