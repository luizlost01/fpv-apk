package com.fpvmirror.decoder

import android.media.MediaCodecInfo
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

/**
 * H.264 video decoder using Android MediaCodec
 * Decodes stream of H.264 NAL units in real-time
 */
class H264Decoder(private val surface: Surface) {
    
    private var mediaCodec: MediaCodec? = null
    private val timeoutUs = 0L  // Non-blocking for high-speed streaming
    private var isRunning = false
    private val bufferInfo = MediaCodec.BufferInfo()
    
    init {
        try {
            initializeCodec()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize decoder: ${e.message}")
        }
    }
    
    private fun initializeCodec() {
        // Create H.264 decoder
        mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE)
        
        // Initial configuration (will be updated by stream SPS/PPS)
        val format = MediaFormat.createVideoFormat(MIME_TYPE, 1920, 1080).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT)
            // Allow larger resolutions for VR mode
            setInteger(MediaFormat.KEY_MAX_WIDTH, 3840)
            setInteger(MediaFormat.KEY_MAX_HEIGHT, 1080)
            // Low latency hints (Android 11+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
        }
        
        mediaCodec?.configure(format, surface, null, 0)
        mediaCodec?.start()
        isRunning = true
        Log.d(TAG, "H.264 decoder started")
    }
    
    /**
     * Decode frame data (Synchronous/Blocking on caller thread)
     */
    fun decodeFrame(data: ByteArray): Boolean {
        if (!isRunning || data.isEmpty()) return false
        
        try {
            val codec = mediaCodec ?: return false
            
            // 1. Feed input
            val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                inputBuffer?.apply {
                    clear()
                    put(data)
                }
                codec.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    data.size,
                    System.nanoTime() / 1000,
                    0
                )
            } else {
                // If input buffer not available, we might be lagging
                Log.w(TAG, "No input buffer available - dropping frame")
            }
            
            // 2. Drain ALL available output buffers to prevent black screen/stalls
            var outputCount = 0
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            while (outputBufferIndex >= 0) {
                codec.releaseOutputBuffer(outputBufferIndex, true)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                outputCount++
            }
            
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                Log.d(TAG, "Format changed: ${newFormat.getInteger(MediaFormat.KEY_WIDTH)}x${newFormat.getInteger(MediaFormat.KEY_HEIGHT)}")
            }
            
            return outputCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Decode error: ${e.message}")
            return false
        }
    }
    
    fun stop() {
        if (isRunning) {
            try {
                isRunning = false
                mediaCodec?.stop()
                mediaCodec?.release()
                mediaCodec = null
                Log.d(TAG, "Decoder stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping decoder: ${e.message}")
            }
        }
    }
    
    companion object {
        private const val TAG = "H264Decoder"
        private const val MIME_TYPE = "video/avc"
        private const val COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    }
}
