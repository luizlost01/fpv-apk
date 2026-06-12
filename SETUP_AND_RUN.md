# FPV Screen Mirror - Complete Setup & Test Guide

This guide will help you build, install, and test the FPV Screen Mirror app on your Android device.

---

## Part 1: Desktop Setup (Computer)

### Step 1.1: Install Python Requirements

```bash
cd ~/fpv-screen-mirror/desktop
pip install -r requirements.txt
```

**Required packages:**
- `mss` - Fast screen capture
- `opencv-python` - Video processing
- `ffmpeg-python` - H.264 encoding (optional, has fallback)

### Step 1.2: Start the Desktop Server

```bash
cd ~/fpv-screen-mirror/desktop
python3 main.py
```

**Expected output:**
```
Starting FPV Screen Mirror Server...
Server listening on 0.0.0.0:5555 (video)
Server listening on 0.0.0.0:5556 (input)
Press Ctrl+C to stop
```

**Keep this terminal open while testing!**

### Step 1.3: Find Your Computer's IP Address

In another terminal, get your desktop IP:

**On Linux/Mac:**
```bash
ifconfig | grep "inet " | grep -v "127.0.0.1"
```

**On Windows:**
```bash
ipconfig | findstr "IPv4"
```

**Example output:**
```
inet 192.168.1.50
```

**Note your IP address: `192.168.1.50` (or your actual IP)**

---

## Part 2: Android Setup (Phone/Tablet)

### Step 2.1: Build the APK

**Option A: Using Android Studio (Recommended)**

1. Open Android Studio
2. Click `File` → `Open` → Select `~/fpv-screen-mirror/android`
3. Wait for Gradle sync to complete
4. Click `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
5. Wait for build to complete (5-10 minutes)
6. APK will be at: `android/app/build/outputs/apk/debug/app-debug.apk`

**Option B: Using Command Line**

```bash
cd ~/fpv-screen-mirror/android
./gradlew assembleDebug
```

**Output:** `android/app/build/outputs/apk/debug/app-debug.apk`

### Step 2.2: Install APK on Phone/Tablet

**Option A: Using Android Studio**

1. Connect your Android device via USB
2. Enable USB Debugging: Settings → Developer Options → USB Debugging
3. In Android Studio: Click `Run` → `Run 'app'` (or press Shift+F10)
4. Select your connected device
5. App will install and launch automatically

**Option B: Using Command Line (ADB)**

```bash
# Verify device is connected
adb devices

# Install the APK
adb install -r ~/fpv-screen-mirror/android/app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.fpvmirror/.MainActivity
```

**Option C: Manual Installation**

1. Transfer APK to your phone via USB cable or email
2. On phone: Files → Find `app-debug.apk`
3. Tap to install → "Install from unknown sources" (if prompted)
4. Open installed app from launcher

### Step 2.3: Connect to WiFi

**Important:** Your phone must be on the same WiFi network as your computer!

1. Phone WiFi Settings → Select your home WiFi
2. Enter password if needed
3. Verify connection is successful

---

## Part 3: Running the Test

### Quick Test (Same Network - WiFi)

**Step 3.1: Start Desktop Server**

```bash
cd ~/fpv-screen-mirror/desktop
python3 main.py
```

**Step 3.2: Launch Android App**

1. On your phone, open the FPV Screen Mirror app
2. Wait for connection status to appear
3. You should see: "Connected - Decoding stereo video"

**Step 3.3: Test Touch Input**

1. Touch the phone screen
2. Move your mouse on the desktop
3. You should see the cursor move
4. Try clicking different areas

**Step 3.4: Stop Testing**

- Press `Ctrl+C` on desktop to stop server
- Or close app on phone

---

## Part 4: Advanced Testing (USB Connection)

If WiFi doesn't work, you can use USB with ADB forwarding:

### Step 4.1: Enable USB Debugging

1. Phone Settings → Developer Options
2. Enable "USB Debugging"
3. Plug phone into computer with USB cable

### Step 4.2: Set Up Port Forwarding

```bash
# Verify device connected
adb devices

# Forward ports
adb forward tcp:5555 tcp:5555
adb forward tcp:5556 tcp:5556
```

### Step 4.3: Start Desktop Server

```bash
cd ~/fpv-screen-mirror/desktop
python3 main.py
```

### Step 4.4: Launch App

On phone, open FPV Screen Mirror app. It should detect USB connection and connect automatically.

---

## Part 5: Troubleshooting

### Issue: "Cannot connect to server"

**Cause 1: Server not running**
- Solution: Check desktop terminal - is server running?
- Restart server with `python3 main.py`

**Cause 2: Phone not on same WiFi**
- Solution: Check phone WiFi settings
- Both devices must be on same network

**Cause 3: Firewall blocking port 5555**
- Solution: Allow port 5555 in firewall settings
- Or use USB forwarding instead

**Cause 4: Wrong server IP**
- Solution: Check desktop IP with `ifconfig`
- Default assumes `x.x.x.100` pattern
- Edit MainActivity.kt line 111 if different

### Issue: "Connected but no video"

**Cause 1: Screen capture not working**
- Check desktop terminal for errors
- Try restarting server

**Cause 2: H.264 encoding failed**
- Check desktop has FFmpeg or OpenCV installed
- Server will fall back to stub encoder for testing

**Cause 3: Decoder initialization failed**
- Check phone logcat: `adb logcat | grep H264Decoder`
- Phone API level must be 21+

### Issue: "Touch input not working"

**Cause 1: Input port not open**
- Desktop server uses port 5556
- Make sure firewall allows it

**Cause 2: Server not receiving input**
- Check desktop logs for input messages
- Verify touch coordinates in logcat

### Issue: "App crashes on startup"

**Cause 1: Incompatible Android version**
- Solution: Minimum API 21 required
- Check phone: Settings → About → Android version

**Cause 2: Missing permissions**
- Solution: Grant WiFi permission when prompted
- Or grant in Settings → Apps → Permissions

**Cause 3: Out of memory**
- Solution: Close other apps
- Try killing app: `adb shell am force-stop com.fpvmirror`

### Issue: "Low frame rate"

**Cause: Network bandwidth limited**
- Solution: Reduce screen resolution on desktop (if possible)
- Use USB connection for faster speeds
- Move closer to WiFi router

---

## Part 6: Testing Checklist

Use this to verify everything is working:

### Desktop Server
- [ ] Server starts without errors
- [ ] Displays "listening on 0.0.0.0:5555"
- [ ] Displays "listening on 0.0.0.0:5556"
- [ ] Can note the IP address

### Android App
- [ ] App installs successfully
- [ ] App launches without crash
- [ ] Fullscreen landscape mode active
- [ ] No error messages displayed

### Connection
- [ ] Server and phone on same WiFi (or USB connected)
- [ ] App shows "Connected" message
- [ ] No connection errors in logcat

### Video
- [ ] Desktop screen appears on phone
- [ ] Image is clear and stable
- [ ] Updates in real-time
- [ ] Stereo layout visible (center divider)

### Touch Input
- [ ] Touch phone screen
- [ ] Desktop mouse moves
- [ ] Multiple touches work
- [ ] Coordinates are accurate

### Performance
- [ ] Frame rate is smooth (at least 30 FPS)
- [ ] No significant lag
- [ ] Video doesn't freeze
- [ ] Touch responds quickly

---

## Part 7: Command Reference

### Desktop Server Commands

```bash
# Start server
cd ~/fpv-screen-mirror/desktop
python3 main.py

# Run with debug logging
python3 main.py --debug

# Stop server (in terminal)
Ctrl+C
```

### Android Commands

```bash
# Build APK
cd ~/fpv-screen-mirror/android
./gradlew assembleDebug

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.fpvmirror/.MainActivity

# View logs
adb logcat | grep fpv

# Uninstall app
adb uninstall com.fpvmirror

# Stop app
adb shell am force-stop com.fpvmirror

# Set up USB forwarding
adb forward tcp:5555 tcp:5555
adb forward tcp:5556 tcp:5556
```

---

## Part 8: Next Steps

Once basic functionality is working:

1. **Measure Performance**
   - How many FPS do you get?
   - What's the latency?
   - Is it smooth enough for your use case?

2. **VR Box Testing**
   - Install app on phone
   - Insert into VR box
   - Test with FPV simulator

3. **Optimize Settings**
   - Adjust resolution if needed
   - Try different WiFi positions
   - Test USB vs WiFi

4. **Provide Feedback**
   - What works well?
   - What needs improvement?
   - Any bugs encountered?

---

## Part 9: File Locations

### Desktop Files
```
~/fpv-screen-mirror/
├── desktop/
│   ├── main.py              # Start here
│   ├── src/
│   │   ├── server.py        # Server implementation
│   │   ├── screen_capture.py
│   │   ├── encoder.py
│   │   ├── protocol.py
│   │   └── ...
│   └── requirements.txt     # Dependencies
├── README.md
└── QUICK_START.md
```

### Android Files
```
~/fpv-screen-mirror/
├── android/
│   ├── gradlew             # Build tool
│   ├── build.gradle        # Project config
│   ├── settings.gradle
│   ├── app/
│   │   ├── build.gradle    # App config
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/com/fpvmirror/  # Source code
│   │   │       │   ├── MainActivity.kt
│   │   │       │   ├── StereoSurfaceView.kt
│   │   │       │   ├── client/
│   │   │       │   ├── decoder/
│   │   │       │   ├── connection/
│   │   │       │   └── network/
│   │   │       ├── res/     # Resources
│   │   │       └── AndroidManifest.xml
│   │   └── build/outputs/apk/debug/app-debug.apk  # Built APK
│   └── ...
└── ...
```

---

## Part 10: Support

If you have issues:

1. **Check the logs**
   ```bash
   # Desktop logs (in terminal running server)
   # Look for error messages
   
   # Android logs
   adb logcat | grep -E "fpv|FPV|Network|Decoder"
   ```

2. **Check documentation**
   - See `README.md` for architecture overview
   - See `ANDROID_BUILD_TEST.md` for detailed build guide
   - See `QUICK_START.md` for quick start

3. **Verify setup**
   - Desktop server running? (Check terminal)
   - Phone on same WiFi? (Check WiFi settings)
   - App permissions granted? (Check phone settings)
   - Port 5555/5556 open? (Check firewall)

---

## Summary

**To run a quick test:**

1. **Desktop:** `python3 ~/fpv-screen-mirror/desktop/main.py`
2. **Android:** Build APK → Install on phone → Open app
3. **Test:** Touch phone screen → see desktop respond

That's it! The app should automatically connect if on same WiFi.

---

**Ready to test?** Start with Part 1 and follow through!

For detailed documentation, see the files in `~/fpv-screen-mirror/docs/`
