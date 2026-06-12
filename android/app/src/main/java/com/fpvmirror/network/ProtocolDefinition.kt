package com.fpvmirror.network

/**
 * Protocol definition for FPV Screen Mirror communication
 * Message format: [type:1][length:4][payload:variable]
 */
object ProtocolDefinition {
    
    // Message types
    const val MSG_HANDSHAKE = 0x01
    const val MSG_HANDSHAKE_ACK = 0x02
    const val MSG_FRAME = 0x03
    const val MSG_INPUT = 0x04
    const val MSG_HEARTBEAT = 0x05
    const val MSG_CONFIG = 0x06
    
    // Default values
    const val DEFAULT_VIDEO_PORT = 5555
    const val DEFAULT_INPUT_PORT = 5556
    const val HEARTBEAT_INTERVAL_MS = 1000
    const val FRAME_TIMEOUT_MS = 5000
}

/**
 * Handshake message to establish connection
 */
data class HandshakeMessage(
    val versionMajor: Int = 1,
    val versionMinor: Int = 0,
    val vrMode: Boolean = true,
    val requestedWidth: Int = 3840,
    val requestedHeight: Int = 1080,
    val targetFps: Int = 60
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(1 + 1 + 1 + 1 + 4 + 4 + 4)
        var pos = 0
        
        buffer[pos++] = versionMajor.toByte()
        buffer[pos++] = versionMinor.toByte()
        buffer[pos++] = if (vrMode) 1 else 0
        buffer[pos++] = 0  // Reserved
        
        // Requested resolution
        requestedWidth.toByteArray().forEachIndexed { i, b -> buffer[pos + i] = b }
        pos += 4
        requestedHeight.toByteArray().forEachIndexed { i, b -> buffer[pos + i] = b }
        pos += 4
        targetFps.toByteArray().forEachIndexed { i, b -> buffer[pos + i] = b }
        
        return buffer
    }
}

/**
 * Touch input message
 */
data class TouchInputMessage(
    val x: Int,
    val y: Int,
    val pressure: Float = 1.0f,
    val action: Int = 0  // 0=down, 1=move, 2=up
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(4 + 4 + 4 + 1)
        var pos = 0
        
        x.toByteArray().forEachIndexed { i, b -> buffer[pos + i] = b }
        pos += 4
        y.toByteArray().forEachIndexed { i, b -> buffer[pos + i] = b }
        pos += 4
        pressure.toBits().toByteArray().forEachIndexed { i, b -> buffer[pos + i] = b }
        pos += 4
        buffer[pos] = action.toByte()
        
        return buffer
    }
}

private fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte()
    )
}

private fun Long.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 56).toByte(),
        (this shr 48).toByte(),
        (this shr 40).toByte(),
        (this shr 32).toByte(),
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte()
    )
}
