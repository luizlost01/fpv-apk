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
    const val DEFAULT_VIDEO_PORT = 5554
    const val DEFAULT_INPUT_PORT = 5556
    const val HEARTBEAT_INTERVAL_MS = 1000
    const val FRAME_TIMEOUT_MS = 5000

    // Frame Header size (within MSG_FRAME payload)
    const val FRAME_HEADER_SIZE = 12 // frame_number(4), timestamp(4), data_size(4)
}

/**
 * Handshake message to establish connection
 * Format: >BBBB HHI (12 bytes)
 */
data class HandshakeMessage(
    val versionMajor: Int = 1,
    val versionMinor: Int = 0,
    val vrMode: Boolean = true,
    val sourceType: Int = 0, // 0 = Desktop Screen, 1 = Camera
    val requestedWidth: Int = 3840,
    val requestedHeight: Int = 1080,
    val targetFps: Int = 60
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(12)
        var pos = 0
        
        buffer[pos++] = versionMajor.toByte()
        buffer[pos++] = versionMinor.toByte()
        buffer[pos++] = if (vrMode) 1 else 0
        buffer[pos++] = sourceType.toByte()
        
        // Requested resolution (Unsigned Short - 2 bytes)
        buffer[pos++] = (requestedWidth shr 8).toByte()
        buffer[pos++] = requestedWidth.toByte()
        
        buffer[pos++] = (requestedHeight shr 8).toByte()
        buffer[pos++] = requestedHeight.toByte()
        
        // Target FPS (Unsigned Int - 4 bytes)
        buffer[pos++] = (targetFps shr 24).toByte()
        buffer[pos++] = (targetFps shr 16).toByte()
        buffer[pos++] = (targetFps shr 8).toByte()
        buffer[pos++] = targetFps.toByte()
        
        return buffer
    }
}

/**
 * Touch input message
 * Format: >i i f B (13 bytes)
 */
data class TouchInputMessage(
    val x: Int,
    val y: Int,
    val pressure: Float = 1.0f,
    val action: Int = 0  // 0=down, 1=move, 2=up
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(13)
        var pos = 0
        
        // X (4 bytes)
        buffer[pos++] = (x shr 24).toByte()
        buffer[pos++] = (x shr 16).toByte()
        buffer[pos++] = (x shr 8).toByte()
        buffer[pos++] = x.toByte()
        
        // Y (4 bytes)
        buffer[pos++] = (y shr 24).toByte()
        buffer[pos++] = (y shr 16).toByte()
        buffer[pos++] = (y shr 8).toByte()
        buffer[pos++] = y.toByte()
        
        // Pressure (4 bytes float)
        val pBits = java.lang.Float.floatToIntBits(pressure)
        buffer[pos++] = (pBits shr 24).toByte()
        buffer[pos++] = (pBits shr 16).toByte()
        buffer[pos++] = (pBits shr 8).toByte()
        buffer[pos++] = pBits.toByte()
        
        // Action (1 byte)
        buffer[pos] = action.toByte()
        
        return buffer
    }
}
