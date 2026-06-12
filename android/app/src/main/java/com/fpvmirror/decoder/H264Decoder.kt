package com.fpvmirror.decoder

import android.media.MediaCodecInfo
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * H.264 video decoder using Android MediaCodec
 * Decodes stream of H.264 NAL units in real-time
 */
class H264Decoder(private val surface: Surface) {
    
    private var mediaCodec: MediaCodec? = null
    private val timeoutUs = 10000L  // 10ms timeout
    private var isRunning = false
    
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
        
        // Configure with stereo resolution (3840x1080)
        val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT)
            setInteger(MediaFormat.KEY_MAX_WIDTH, VIDEO_WIDTH)
            setInteger(MediaFormat.KEY_MAX_HEIGHT, VIDEO_HEIGHT)
        }
        
        mediaCodec?.configure(format, surface, null, 0)
        mediaCodec?.start()
        isRunning = true
        Log.d(TAG, "H.264 decoder initialized (${VIDEO_WIDTH}x${VIDEO_HEIGHT})")
    }
    
    suspend fun decodeFrame(data: ByteArray): Boolean = withContext(Dispatchers.Default) {
        if (!isRunning || data.isEmpty()) return@withContext false
        
        try {
            val codec = mediaCodec ?: return@withContext false
            
            // Get input buffer
            val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex < 0) return@withContext false
            
            val inputBuffer = codec.getInputBuffer(inputBufferIndex)
            inputBuffer?.apply {
                clear()
                put(data)
                flip()
            }
            
            // Queue the input buffer with frame data
            codec.queueInputBuffer(
                inputBufferIndex,
                0,
                data.size,
                System.nanoTime() / 1000,  // presentation time in microseconds
                0
            )
            
            // Get and render output
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            
            return@withContext when {
                outputBufferIndex >= 0 -> {
                    codec.releaseOutputBuffer(outputBufferIndex, true)
                    true
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d(TAG, "Decoder output format changed")
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decode error: ${e.message}")
            false
        }
    }
    
    fun stop() {
        if (isRunning) {
            try {
                mediaCodec?.stop()
                mediaCodec?.release()
                mediaCodec = null
                isRunning = false
                Log.d(TAG, "Decoder stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping decoder: ${e.message}")
            }
        }
    }
    
    companion object {
        private const val TAG = "H264Decoder"
        private const val MIME_TYPE = "video/avc"  // H.264
        private const val VIDEO_WIDTH = 3840
        private const val VIDEO_HEIGHT = 1080
        // FIXED: Removed the invalid MediaFormat prefix
        private const val COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    }
}