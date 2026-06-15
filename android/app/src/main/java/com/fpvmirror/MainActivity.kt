package com.fpvmirror

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fpvmirror.client.FPVClient
import com.fpvmirror.connection.WiFiConnectionManager
import kotlinx.coroutines.launch

/**
 * Main activity for FPV Screen Mirror
 * Displays selection screen and then stereo VR screen
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var stereoView: StereoSurfaceView
    private lateinit var fpvClient: FPVClient
    private lateinit var wifiManager: WiFiConnectionManager
    
    private lateinit var rootLayout: FrameLayout
    private lateinit var selectionLayout: LinearLayout
    private lateinit var vrModeCheckBox: CheckBox
    
    private var isConnecting = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        fpvClient = FPVClient()
        wifiManager = WiFiConnectionManager(this)
        
        // Create main layout
        rootLayout = FrameLayout(this)
        
        // Create stereo view (initially hidden)
        stereoView = StereoSurfaceView(this)
        stereoView.visibility = View.GONE
        rootLayout.addView(stereoView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // Create selection UI
        selectionLayout = createSelectionUI()
        rootLayout.addView(selectionLayout)
        
        setContentView(rootLayout)
        
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
            runOnUiThread {
                stereoView.setConnected(connected)
                if (connected) {
                    showVideoView()
                } else {
                    showSelectionView()
                }
                showToast(message)
            }
        }
        
        fpvClient.setErrorCallback { error ->
            showToast(error)
            runOnUiThread { showSelectionView() }
        }
        
        // Request fullscreen and landscape
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun createSelectionUI(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            addView(TextView(context).apply {
                text = "FPV Screen Mirror"
                textSize = 32f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 60)
            })

            // VR Mode Selection
            vrModeCheckBox = CheckBox(context).apply {
                text = "Enable VR Mode (Stereo SBS)"
                setTextColor(Color.WHITE)
                isChecked = true
                setPadding(0, 0, 0, 40)
            }
            addView(vrModeCheckBox)

            val buttonParams = LinearLayout.LayoutParams(600, 150).apply {
                setMargins(0, 20, 0, 20)
            }

            addView(Button(context).apply {
                text = "WiFi Connection"
                layoutParams = buttonParams
                setOnClickListener { startWiFiConnection() }
            })

            addView(Button(context).apply {
                text = "USB ADB Connection"
                layoutParams = buttonParams
                setOnClickListener { startUSBConnection() }
            })
            
            addView(TextView(context).apply {
                text = "Mirroring Desktop Screen by default"
                textSize = 14f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 0)
            })
        }
    }

    private fun startWiFiConnection() {
        if (isConnecting) return
        isConnecting = true
        val isVR = vrModeCheckBox.isChecked
        stereoView.setMode(isVR)
        showToast("Searching for WiFi server...")
        
        lifecycleScope.launch {
            try {
                if (wifiManager.isWiFiConnected()) {
                    wifiManager.startServerDiscovery { host, port ->
                        lifecycleScope.launch {
                            val connected = fpvClient.connectViaWiFi(host, port, isVR)
                            isConnecting = false
                            if (!connected) {
                                showToast("Failed to connect to WiFi server")
                                showSelectionView()
                            }
                        }
                    }
                } else {
                    showToast("WiFi not connected")
                    isConnecting = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "WiFi error: ${e.message}")
                showToast("Error: ${e.message}")
                isConnecting = false
            }
        }
    }

    private fun startUSBConnection() {
        if (isConnecting) return
        isConnecting = true
        val isVR = vrModeCheckBox.isChecked
        stereoView.setMode(isVR)
        showToast("Attempting USB connection...")
        
        lifecycleScope.launch {
            try {
                val connected = fpvClient.connectViaUSB(WiFiConnectionManager.DEFAULT_VIDEO_PORT, isVR)
                isConnecting = false
                if (!connected) {
                    showToast("USB connection failed. Ensure 'adb reverse' is configured.")
                    showSelectionView()
                }
            } catch (e: Exception) {
                Log.e(TAG, "USB error: ${e.message}")
                showToast("Error: ${e.message}")
                isConnecting = false
            }
        }
    }

    private fun showVideoView() {
        stereoView.visibility = View.VISIBLE
        selectionLayout.visibility = View.GONE
    }

    private fun showSelectionView() {
        stereoView.visibility = View.GONE
        selectionLayout.visibility = View.VISIBLE
    }
    
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
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
    
    companion object {
        private const val TAG = "MainActivity"
    }
}
