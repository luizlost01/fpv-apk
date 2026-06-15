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

    // Core background scope for network IO tasks
    private val ioScope = CoroutineScope(Dispatchers.IO)
    // Dedicated UI scope to safely route lifecycle changes back to the main thread
    private val uiScope = CoroutineScope(Dispatchers.Main)

    // Callbacks
    private var onFrameReceived: ((ByteArray) -> Unit)? = null
    private var onConnectionStatusChanged: ((Boolean, String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    /**
     * Connect to FPV server via WiFi
     */
    suspend fun connectViaWiFi(
        host: String, 
        port: Int = ProtocolDefinition.DEFAULT_VIDEO_PORT,
        isVRMode: Boolean = true
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting via WiFi to $host:$port")

                networkClient = NetworkClient(host, port).apply {
                    setFrameCallback { data -> onFrameReceived?.invoke(data) }
                    setStatusCallback { connected ->
                        uiScope.launch {
                            onConnectionStatusChanged?.invoke(
                                connected,
                                if (connected) "WiFi Connected" else "WiFi Disconnected"
                            )
                        }
                    }
                    setErrorCallback { error ->
                        uiScope.launch {
                            onError?.invoke("WiFi Error: $error")
                        }
                    }
                }

                val connected = networkClient?.connect() ?: false
                if (connected) {
                    val handshake = HandshakeMessage(
                        vrMode = isVRMode,
                        sourceType = 0, // 0 = Desktop Screen
                        requestedWidth = if (isVRMode) 3840 else 1920,
                        requestedHeight = 1080,
                        targetFps = 60
                    )
                    networkClient?.sendHandshake(handshake)

                    ioScope.launch {
                        networkClient?.startReceivingFrames()
                    }

                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "WiFi connection error: ${e.message}")
                return@withContext false
            }
        }
    }

    /**
     * Connect via USB ADB
     */
    suspend fun connectViaUSB(
        port: Int = ProtocolDefinition.DEFAULT_VIDEO_PORT,
        isVRMode: Boolean = true
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting via USB ADB to localhost:$port")

                networkClient = NetworkClient("127.0.0.1", port).apply {
                    setFrameCallback { data -> onFrameReceived?.invoke(data) }
                    setStatusCallback { connected ->
                        uiScope.launch {
                            onConnectionStatusChanged?.invoke(
                                connected,
                                if (connected) "USB Connected" else "USB Disconnected"
                            )
                        }
                    }
                    setErrorCallback { error ->
                        uiScope.launch {
                            onError?.invoke("USB Error: $error")
                        }
                    }
                }

                val connected = networkClient?.connect() ?: false
                if (connected) {
                    val handshake = HandshakeMessage(
                        vrMode = isVRMode,
                        sourceType = 0, // 0 = Desktop Screen
                        requestedWidth = if (isVRMode) 3840 else 1920,
                        requestedHeight = 1080,
                        targetFps = 60
                    )
                    networkClient?.sendHandshake(handshake)

                    ioScope.launch {
                        networkClient?.startReceivingFrames()
                    }

                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "USB connection error: ${e.message}")
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
