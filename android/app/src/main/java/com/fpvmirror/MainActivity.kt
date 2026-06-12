package com.fpvmirror

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fpvmirror.client.FPVClient
import com.fpvmirror.connection.WiFiConnectionManager
import com.fpvmirror.network.ProtocolDefinition
import kotlinx.coroutines.launch

/**
 * Main activity for FPV Screen Mirror
 * Displays stereo VR screen in landscape mode with frame decoding
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var stereoView: StereoSurfaceView
    private lateinit var fpvClient: FPVClient
    private lateinit var wifiManager: WiFiConnectionManager
    private var isVRMode = true
    private var isConnecting = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        fpvClient = FPVClient()
        wifiManager = WiFiConnectionManager(this)
        
        // Create main layout
        val mainLayout = FrameLayout(this)
        mainLayout.id = android.R.id.content
        
        // Create stereo view
        stereoView = StereoSurfaceView(this)
        mainLayout.addView(stereoView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        setContentView(mainLayout)
        
        // Set touch listener for input
        stereoView.setTouchListener { x, y ->
            lifecycleScope.launch {
                fpvClient.sendTouchInput(x, y, 1)
            }
        }
        
        // Set status listener
        stereoView.setStatusChangeListener { status ->
            Log.d(TAG, status)
        }
        
        // Set up FPV client callbacks
        fpvClient.setFrameCallback { frameData ->
            stereoView.decodeFrame(frameData)
        }
        
        fpvClient.setStatusCallback { connected, message ->
            stereoView.setConnected(connected)
            showToast(message)
        }
        
        fpvClient.setErrorCallback { error ->
            showToast(error)
        }
        
        // Request fullscreen and landscape
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Initialize connection
        initializeConnection()
    }
    
    private fun initializeConnection() {
        lifecycleScope.launch {
            try {
                if (isConnecting) return@launch
                isConnecting = true
                
                // Check WiFi availability first
                if (wifiManager.isWiFiConnected()) {
                    Log.d(TAG, "WiFi is connected, attempting WiFi connection...")
                    
                    // Try to discover server via mDNS
                    val localIP = wifiManager.getLocalIP()
                    if (localIP != null) {
                        Log.d(TAG, "Local IP: $localIP")
                        // Try default server port
                        val serverIP = localIP.substringBeforeLast(".") + ".100"  // Assume server is at x.x.x.100
                        
                        val connected = fpvClient.connectViaWiFi(
                            serverIP,
                            WiFiConnectionManager.DEFAULT_VIDEO_PORT
                        )
                        
                        if (connected) {
                            Log.d(TAG, "Connected via WiFi")
                            return@launch
                        }
                    }
                } else {
                    Log.d(TAG, "WiFi not connected, attempting USB ADB connection...")
                    
                    // Fall back to USB ADB
                    val connected = fpvClient.connectViaUSB(WiFiConnectionManager.DEFAULT_VIDEO_PORT)
                    if (connected) {
                        Log.d(TAG, "Connected via USB ADB")
                        return@launch
                    }
                }
                
                showToast("Failed to connect. Please check connection or start desktop server.")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization error: ${e.message}")
                showToast("Error: ${e.message}")
            } finally {
                isConnecting = false
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            fpvClient.disconnect()
            stereoView.cleanup()
        }
    }
    
    override fun onPause() {
        super.onPause()
        wifiManager.stopServerDiscovery()
    }
    
    override fun onResume() {
        super.onResume()
        if (!fpvClient.isConnected() && !isConnecting) {
            initializeConnection()
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}
