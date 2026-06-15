package com.fpvmirror.connection

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WiFi connection manager for discovering and connecting to FPV Screen Mirror server
 */
class WiFiConnectionManager(context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: DiscoveryListener? = null

    // Use an AtomicBoolean flag to ensure we never run concurrent service resolutions
    private val isResolving = AtomicBoolean(false)
    private var onServerFound: ((String, Int) -> Unit)? = null

    /**
     * Start discovering FPV Screen Mirror servers on the network
     */
    fun startServerDiscovery(onFound: (String, Int) -> Unit) {
        onServerFound = onFound
        isResolving.set(false)

        discoveryListener = DiscoveryListener()

        try {
            nsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
            Log.d(TAG, "Started discovering FPV servers...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery: ${e.message}")
        }
    }

    /**
     * Stop discovering servers
     */
    fun stopServerDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery: ${e.message}")
            } finally {
                discoveryListener = null
                isResolving.set(false)
            }
        }
    }

    private inner class DiscoveryListener : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service found: ${service.serviceName}")

            // Thread-safe guard: Only resolve if there isn't an active resolution ongoing
            if (isResolving.compareAndSet(false, true)) {
                val resolveListener = OneShotResolveListener()
                nsdManager.resolveService(service, resolveListener)
            } else {
                Log.w(TAG, "Resolution ignored: Another service resolution is already in progress.")
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "Service lost: ${service.serviceName}")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "Discovery stopped")
            isResolving.set(false)
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Stop discovery failed: $errorCode")
        }
    }

    // Dedicated one-shot listener instance per resolution to avoid overwriting state
    private inner class OneShotResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Resolve failed: $errorCode")
            isResolving.set(false) // Reset flag on failure so we can try next service
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service resolved: ${serviceInfo.serviceName}")

            val host = serviceInfo.host?.hostAddress
            val port = serviceInfo.port

            isResolving.set(false) // Reset flag safely after extraction

            if (host != null) {
                Log.d(TAG, "Found server: $host:$port")
                onServerFound?.invoke(host, port)
            }
        }
    }

    /**
     * Get the device's local IP address
     */
    suspend fun getLocalIP(): String? = withContext(Dispatchers.Default) {
        try {
            val connectionInfo = wifiManager.connectionInfo ?: return@withContext null
            val ip = connectionInfo.ipAddress
            if (ip == 0) return@withContext null

            return@withContext String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Check if WiFi is connected
     */
    fun isWiFiConnected(): Boolean {
        return try {
            val connectionInfo = wifiManager.connectionInfo
            connectionInfo != null && connectionInfo.ipAddress != 0
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "WiFiConnectionManager"
        private const val SERVICE_TYPE = "_fpvmirror._tcp."

        const val DEFAULT_VIDEO_PORT = 5554
        const val DEFAULT_INPUT_PORT = 5556
    }
}