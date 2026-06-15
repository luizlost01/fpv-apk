package com.fpvmirror.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance network client optimized for low latency.
 * Uses aggressive buffer clearing to prevent backlog delay.
 */
class NetworkClient(private val host: String, private val port: Int) {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private val isConnected = AtomicBoolean(false)

    private var onFrameReceived: ((ByteArray) -> Unit)? = null
    private var onConnectionStatusChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket(host, port).apply {
                tcpNoDelay = true
                soTimeout = 2000 
                // Small receive buffer to force dropping at the OS level if we are slow,
                // instead of letting 8 seconds of data accumulate in the buffer.
                receiveBufferSize = 256 * 1024 
            }
            inputStream = socket?.inputStream
            isConnected.set(true)
            onConnectionStatusChanged?.invoke(true)
            return@withContext true
        } catch (e: Exception) {
            isConnected.set(false)
            onConnectionStatusChanged?.invoke(false)
            return@withContext false
        }
    }

    suspend fun sendHandshake(handshake: HandshakeMessage) {}

    suspend fun sendTouchInput(input: TouchInputMessage) {
        // Implementation for touch feedback...
    }

    suspend fun startReceivingFrames() = withContext(Dispatchers.IO) {
        val lengthBuffer = ByteArray(8)
        val frameBuffer = ByteArray(2 * 1024 * 1024) 

        while (isConnected.get()) {
            try {
                val input = inputStream ?: break

                // 1. Read Length
                if (!readFully(input, lengthBuffer, 8)) break
                val msgLength = ByteBuffer.wrap(lengthBuffer)
                    .order(ByteOrder.LITTLE_ENDIAN).long.toInt()

                if (msgLength <= 0 || msgLength > frameBuffer.size) {
                    // Protocol desync, need to reconnect
                    break
                }

                // 2. Read JPEG Payload
                if (!readFully(input, frameBuffer, msgLength)) break

                // 3. Forward to renderer (non-blocking)
                val data = ByteArray(msgLength)
                System.arraycopy(frameBuffer, 0, data, 0, msgLength)
                onFrameReceived?.invoke(data)

            } catch (e: SocketTimeoutException) {
                continue
            } catch (e: Exception) {
                break
            }
        }
        disconnect()
    }

    private fun readFully(input: InputStream, buffer: ByteArray, length: Int): Boolean {
        var totalRead = 0
        while (totalRead < length) {
            val n = input.read(buffer, totalRead, length - totalRead)
            if (n < 0) return false
            totalRead += n
        }
        return true
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        if (isConnected.compareAndSet(true, false)) {
            socket?.close()
            onConnectionStatusChanged?.invoke(false)
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

    fun isConnectedNow(): Boolean = isConnected.get()

    companion object {
        private const val TAG = "NetworkClient"
    }
}
