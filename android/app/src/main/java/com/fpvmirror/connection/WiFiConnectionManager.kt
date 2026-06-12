package com.fpvmirror.connection

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WiFi connection manager for discovering and connecting to FPV Screen Mirror server
 */
class WiFiConnectionManager(context: Context) {
    
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    
    private var discoveryListener: DiscoveryListener? = null
    private var resolveListener: ResolveListener? = null
    private var onServerFound: ((String, Int) -> Unit)? = null
    
    /**
     * Start discovering FPV Screen Mirror servers on the network
     */
    fun startServerDiscovery(onFound: (String, Int) -> Unit) {
        onServerFound = onFound
        
        // Correctly instantiate the inner class
        discoveryListener = DiscoveryListener()
        
        nsdManager.discoverServices(
            SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
        
        Log.d(TAG, "Started discovering FPV servers...")
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
            }
        }
    }
    
    private inner class DiscoveryListener : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Discovery started")
        }
        
        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service found: ${service.serviceName}")
            
            // Resolve service details
            resolveListener = ResolveListener()
            nsdManager.resolveService(service, resolveListener)
        }
        
        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "Service lost: ${service.serviceName}")
        }
        
        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "Discovery stopped")
        }
        
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: $errorCode")
        }
        
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Stop discovery failed: $errorCode")
        }
    }
    
    private inner class ResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Resolve failed: $errorCode")
        }
        
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service resolved: ${serviceInfo.serviceName}")
            
            val host = serviceInfo.host.hostAddress
            val port = serviceInfo.port
            
            Log.d(TAG, "Found server: $host:$port")
            onServerFound?.invoke(host, port)
        }
    }
    
    /**
     * Get the device's local IP address
     */
    suspend fun getLocalIP(): String? = withContext(Dispatchers.Default) {
        try {
            val connectionInfo = wifiManager.connectionInfo
            val ip = connectionInfo.ipAddress
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
        private const val SERVICE_NAME = "FPV Screen Mirror"
        
        const val DEFAULT_VIDEO_PORT = 5555
        const val DEFAULT_INPUT_PORT = 5556
    }
}