package com.fpvmirror

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import com.fpvmirror.decoder.H264Decoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom TextureView for rendering stereo VR screen mirror
 * Uses H.264 decoder for real-time video decoding
 * Displays side-by-side stereo: left eye | right eye
 */
class StereoSurfaceView(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs), TextureView.SurfaceTextureListener {
    
    private val paint = Paint()
    private var decoder: H264Decoder? = null
    private var isConnected = false
    private var frameCount = 0
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private var touchListener: ((x: Int, y: Int) -> Unit)? = null
    private var onStatusChanged: ((String) -> Unit)? = null
    
    init {
        surfaceTextureListener = this
        setBackgroundColor(Color.BLACK)
    }
    
    /**
     * Decode and display H.264 frame
     */
    fun decodeFrame(data: ByteArray) {
        coroutineScope.launch {
            decoder?.let {
                val success = it.decodeFrame(data)
                if (success) {
                    frameCount++
                    if (frameCount % 60 == 0) {
                        Log.d(TAG, "Decoded $frameCount frames")
                    }
                } else {
                    Log.w(TAG, "Frame decode failed")
                }
            }
        }
    }
    
    fun setConnected(connected: Boolean) {
        isConnected = connected
        if (connected) {
            onStatusChanged?.invoke("Connected - Decoding stereo video")
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
        
        try {
            decoder = H264Decoder(android.view.Surface(surface))
            onStatusChanged?.invoke("Decoder initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create decoder: ${e.message}")
            onStatusChanged?.invoke("Decoder error: ${e.message}")
        }
    }
    
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.d(TAG, "SurfaceTexture destroyed")
        decoder?.stop()
        decoder = null
        return true
    }
    
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "SurfaceTexture size changed: ${width}x$height")
    }
    
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Called when new frame is available - rendering happens automatically
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x.toInt()
                val touchY = event.y.toInt()
                
                // Map touch coordinates accounting for stereo layout
                // Screen is 3840 wide (1920 left + 1920 right)
                val mappedX = if (touchX > width / 2) {
                    touchX - (width / 2)  // Right eye touch, map to second half
                } else {
                    touchX  // Left eye touch
                }
                
                touchListener?.invoke(mappedX, touchY)
            }
        }
        return true
    }
    
    fun cleanup() {
        decoder?.stop()
        decoder = null
    }
    
    companion object {
        private const val TAG = "StereoSurfaceView"
    }
}
