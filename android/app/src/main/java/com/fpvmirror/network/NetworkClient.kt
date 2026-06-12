package com.fpvmirror.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Network client for WiFi/USB communication with desktop server
 */
class NetworkClient(private val host: String, private val port: Int) {
    
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    
    // Callbacks
    private var onFrameReceived: ((ByteArray) -> Unit)? = null
    private var onConnectionStatusChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket(host, port).apply {
                soTimeout = ProtocolDefinition.FRAME_TIMEOUT_MS
            }
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            isConnected = true
            onConnectionStatusChanged?.invoke(true)
            Log.d(TAG, "Connected to $host:$port")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            onError?.invoke("Failed to connect: ${e.message}")
            isConnected = false
            onConnectionStatusChanged?.invoke(false)
            return@withContext false
        }
    }
    
    suspend fun sendHandshake(handshake: HandshakeMessage) = withContext(Dispatchers.IO) {
        try {
            val data = handshake.toByteArray()
            outputStream?.write(ProtocolDefinition.MSG_HANDSHAKE.toByte().toInt())
            outputStream?.write(data.size shr 24)
            outputStream?.write(data.size shr 16)
            outputStream?.write(data.size shr 8)
            outputStream?.write(data.size)
            outputStream?.write(data)
            outputStream?.flush()
            Log.d(TAG, "Handshake sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending handshake: ${e.message}")
            onError?.invoke("Handshake failed: ${e.message}")
        }
    }
    
    suspend fun sendTouchInput(input: TouchInputMessage) = withContext(Dispatchers.IO) {
        try {
            val data = input.toByteArray()
            outputStream?.write(ProtocolDefinition.MSG_INPUT.toByte().toInt())
            outputStream?.write(data.size shr 24)
            outputStream?.write(data.size shr 16)
            outputStream?.write(data.size shr 8)
            outputStream?.write(data.size)
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending input: ${e.message}")
        }
    }
    
    suspend fun startReceivingFrames() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val header = ByteArray(5)
                val buffer = ByteArray(256 * 1024)  // 256KB buffer for video frames
                var totalFrames = 0
                
                while (isConnected) {
                    try {
                        val input = inputStream ?: break
                        
                        // Read message header (1 byte type + 4 bytes length)
                        var bytesRead = 0
                        while (bytesRead < 5 && isConnected) {
                            val n = input.read(header, bytesRead, 5 - bytesRead)
                            if (n < 0) {
                                disconnect()
                                break
                            }
                            bytesRead += n
                        }
                        
                        if (bytesRead < 5) break
                        
                        val msgType = header[0].toInt() and 0xFF
                        val msgLength = ((header[1].toInt() and 0xFF) shl 24) or
                                       ((header[2].toInt() and 0xFF) shl 16) or
                                       ((header[3].toInt() and 0xFF) shl 8) or
                                       (header[4].toInt() and 0xFF)
                        
                        // Read message payload
                        if (msgLength > buffer.size) {
                            Log.w(TAG, "Message too large: $msgLength bytes")
                            continue
                        }
                        
                        bytesRead = 0
                        while (bytesRead < msgLength && isConnected) {
                            val n = input.read(buffer, bytesRead, msgLength - bytesRead)
                            if (n < 0) {
                                disconnect()
                                break
                            }
                            bytesRead += n
                        }
                        
                        if (bytesRead < msgLength) break
                        
                        // Process message based on type
                        when (msgType) {
                            ProtocolDefinition.MSG_FRAME -> {
                                val frameData = buffer.sliceArray(0 until msgLength)
                                onFrameReceived?.invoke(frameData)
                                totalFrames++
                                if (totalFrames % 60 == 0) {
                                    Log.d(TAG, "Received $totalFrames frames")
                                }
                            }
                            else -> Log.w(TAG, "Unknown message type: $msgType")
                        }
                    } catch (e: SocketTimeoutException) {
                        Log.w(TAG, "Socket timeout, continuing...")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error receiving frames: ${e.message}")
                onError?.invoke("Connection lost: ${e.message}")
                disconnect()
            }
        }
    }
    
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            isConnected = false
            outputStream?.close()
            inputStream?.close()
            socket?.close()
            onConnectionStatusChanged?.invoke(false)
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }
    
    fun setFrameCallback(callback: (ByteArray) -> Unit) {
        onFrameReceived = callback
    }
    
    fun setStatusCallback(callback: (Boolean) -> Unit) {
        onConnectionStatusChanged = callback
    }
    
    fun setErrorCallback(callback: (String) -> Unit) {
        onError = callback
    }
    
    fun isConnectedNow(): Boolean = isConnected
    
    companion object {
        private const val TAG = "NetworkClient"
    }
}
