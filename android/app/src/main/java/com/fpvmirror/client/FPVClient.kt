package com.fpvmirror.client

import android.util.Log
import com.fpvmirror.network.NetworkClient
import com.fpvmirror.network.ProtocolDefinition
import com.fpvmirror.network.HandshakeMessage
import com.fpvmirror.network.TouchInputMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified client manager for FPV Screen Mirror
 * Handles WiFi and USB connections with automatic fallback
 */
class FPVClient {
    
    private var networkClient: NetworkClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Callbacks
    private var onFrameReceived: ((ByteArray) -> Unit)? = null
    private var onConnectionStatusChanged: ((Boolean, String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    
    /**
     * Connect to FPV server via WiFi
     */
    suspend fun connectViaWiFi(host: String, port: Int = ProtocolDefinition.DEFAULT_VIDEO_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting via WiFi to $host:$port")
                
                networkClient = NetworkClient(host, port).apply {
                    setStatusCallback { connected ->
                        coroutineScope.launch {
                            onConnectionStatusChanged?.invoke(connected, if (connected) "WiFi Connected" else "WiFi Disconnected")
                        }
                    }
                    setErrorCallback { error ->
                        onError?.invoke("WiFi Error: $error")
                    }
                }
                
                val connected = networkClient?.connect() ?: false
                if (connected) {
                    // Send handshake
                    val handshake = HandshakeMessage(
                        vrMode = true,
                        requestedWidth = 3840,
                        requestedHeight = 1080,
                        targetFps = 60
                    )
                    networkClient?.sendHandshake(handshake)
                    
                    // Start receiving frames
                    networkClient?.startReceivingFrames()
                    
                    Log.d(TAG, "WiFi connection successful")
                    onConnectionStatusChanged?.invoke(true, "Connected via WiFi")
                    return@withContext true
                } else {
                    Log.e(TAG, "WiFi connection failed")
                    onError?.invoke("Failed to connect via WiFi")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "WiFi connection error: ${e.message}")
                onError?.invoke("WiFi error: ${e.message}")
                return@withContext false
            }
        }
    }
    
    /**
     * Connect via USB ADB
     * Requires device to be connected via USB with ADB forwarding set up:
     * adb forward tcp:5555 tcp:5555
     */
    suspend fun connectViaUSB(port: Int = ProtocolDefinition.DEFAULT_VIDEO_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting via USB ADB to localhost:$port")
                
                networkClient = NetworkClient("127.0.0.1", port).apply {
                    setStatusCallback { connected ->
                        coroutineScope.launch {
                            onConnectionStatusChanged?.invoke(connected, if (connected) "USB Connected" else "USB Disconnected")
                        }
                    }
                    setErrorCallback { error ->
                        onError?.invoke("USB Error: $error")
                    }
                }
                
                val connected = networkClient?.connect() ?: false
                if (connected) {
                    // Send handshake
                    val handshake = HandshakeMessage(
                        vrMode = true,
                        requestedWidth = 3840,
                        requestedHeight = 1080,
                        targetFps = 60
                    )
                    networkClient?.sendHandshake(handshake)
                    
                    // Start receiving frames
                    networkClient?.startReceivingFrames()
                    
                    Log.d(TAG, "USB connection successful")
                    onConnectionStatusChanged?.invoke(true, "Connected via USB")
                    return@withContext true
                } else {
                    Log.e(TAG, "USB connection failed")
                    onError?.invoke("Failed to connect via USB")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "USB connection error: ${e.message}")
                onError?.invoke("USB error: ${e.message}")
                return@withContext false
            }
        }
    }
    
    /**
     * Send touch input to server
     */
    suspend fun sendTouchInput(x: Int, y: Int, action: Int = 1) {
        networkClient?.let {
            val input = TouchInputMessage(x, y, action = action)
            it.sendTouchInput(input)
        }
    }
    
    /**
     * Disconnect from server
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                networkClient?.disconnect()
                networkClient = null
                Log.d(TAG, "Disconnected")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting: ${e.message}")
            }
        }
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return networkClient?.isConnectedNow() ?: false
    }
    
    /**
     * Set callbacks
     */
    fun setFrameCallback(callback: (ByteArray) -> Unit) {
        onFrameReceived = callback
        networkClient?.setFrameCallback(callback)
    }
    
    fun setStatusCallback(callback: (Boolean, String) -> Unit) {
        onConnectionStatusChanged = callback
    }
    
    fun setErrorCallback(callback: (String) -> Unit) {
        onError = callback
    }
    
    companion object {
        private const val TAG = "FPVClient"
    }
}
