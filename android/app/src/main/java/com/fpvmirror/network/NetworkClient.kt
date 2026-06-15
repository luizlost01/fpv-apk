package com.fpvmirror.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Network client for WiFi/USB communication with desktop server
 * Updated to match the "Phase 1" JPEG server protocol:
 * [8 bytes Length (Little Endian)][JPEG Data]
 */
class NetworkClient(private val host: String, private val port: Int) {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val isConnected = AtomicBoolean(false)

    // Callbacks
    private var onFrameReceived: ((ByteArray) -> Unit)? = null
    private var onConnectionStatusChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port...")
            socket = Socket(host, port).apply {
                soTimeout = ProtocolDefinition.FRAME_TIMEOUT_MS
                tcpNoDelay = true
                receiveBufferSize = 4 * 1024 * 1024 // 4MB for JPEGs
            }
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            isConnected.set(true)
            onConnectionStatusChanged?.invoke(true)
            Log.d(TAG, "Connected successfully to $host:$port")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            onError?.invoke("Failed to connect: ${e.message}")
            isConnected.set(false)
            onConnectionStatusChanged?.invoke(false)
            return@withContext false
        }
    }

    /**
     * Handshake is skipped in Phase 1 protocol, but kept for future compatibility
     */
    suspend fun sendHandshake(handshake: HandshakeMessage) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Skipping handshake for Phase 1 protocol")
    }

    suspend fun sendTouchInput(input: TouchInputMessage) = withContext(Dispatchers.IO) {
        // Input server might not be implemented on desktop yet, but we send anyway
        try {
            val data = input.toByteArray()
            outputStream?.let { stream ->
                stream.write(data)
                stream.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending input: ${e.message}")
        }
    }

    suspend fun startReceivingFrames() = withContext(Dispatchers.IO) {
        try {
            val lengthBuffer = ByteArray(8)
            val bufferSize = 4 * 1024 * 1024
            val buffer = ByteArray(bufferSize)
            
            Log.d(TAG, "Started receiving JPEG frames...")

            while (isConnected.get()) {
                try {
                    val input = inputStream ?: break

                    // 1. Read 8-byte Little Endian Length (Python 'Q')
                    if (!readFully(input, lengthBuffer, 8)) break

                    val msgLength = ByteBuffer.wrap(lengthBuffer)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .long

                    if (msgLength <= 0 || msgLength > 10 * 1024 * 1024) {
                        Log.e(TAG, "Invalid JPEG length: $msgLength")
                        continue
                    }

                    // 2. Read JPEG Payload
                    val len = msgLength.toInt()
                    if (len > bufferSize) {
                        Log.w(TAG, "JPEG too large ($len), skipping...")
                        skipFully(input, msgLength)
                        continue
                    }

                    if (!readFully(input, buffer, len)) break

                    // 3. Emit Frame
                    val frameData = ByteArray(len)
                    System.arraycopy(buffer, 0, frameData, 0, len)
                    onFrameReceived?.invoke(frameData)

                } catch (e: SocketTimeoutException) {
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Receive loop error: ${e.message}")
            onError?.invoke("Connection lost: ${e.message}")
            disconnect()
        }
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

    private fun skipFully(input: InputStream, length: Long) {
        var totalSkipped = 0L
        while (totalSkipped < length) {
            val skipped = input.skip(length - totalSkipped)
            if (skipped <= 0) break
            totalSkipped += skipped
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        if (isConnected.compareAndSet(true, false)) {
            try {
                outputStream?.close()
                inputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect: ${e.message}")
            } finally {
                outputStream = null
                inputStream = null
                socket = null
                onConnectionStatusChanged?.invoke(false)
                Log.d(TAG, "Disconnected")
            }
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
