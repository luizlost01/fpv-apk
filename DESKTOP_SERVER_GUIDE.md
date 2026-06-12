# FPV Screen Mirror - Desktop Server User Guide

This guide explains how to run and use the desktop server on your computer.

---

## Quick Start (Fastest Way)

### 1️⃣ Install Python Dependencies

Open a terminal and run:

```bash
cd ~/fpv-screen-mirror/desktop
pip install -r requirements.txt
```

**Takes 1-2 minutes first time**

### 2️⃣ Start the Server

```bash
python3 main.py
```

**You should see:**
```
Starting FPV Screen Mirror Server...
Server listening on 0.0.0.0:5555 (video stream)
Server listening on 0.0.0.0:5556 (input/touch)
Press Ctrl+C to stop
```

### 3️⃣ Keep Terminal Open

**Don't close this terminal!** The server needs to stay running while you use the app on your phone.

---

## What the Server Does

The desktop server:

1. **Captures your screen** - Takes a screenshot 60 times per second
2. **Encodes to H.264** - Compresses the video for fast transmission
3. **Broadcasts to phone** - Sends video stream to your Android device
4. **Receives touches** - Gets touch input from your phone and moves your mouse

```
Your Computer Screen
        ↓
    [Capture]
        ↓
    [Encode to H.264]
        ↓
    [Send to Phone]
        ↓
    [Phone Screen]
        ↓
    [You touch phone]
        ↓
    [Send touch to computer]
        ↓
    [Mouse moves on your computer]
```

---

## Server Configuration

### Change Default Ports

Edit `desktop/src/server.py`:

```python
FRAME_PORT = 5555        # Video streaming port
INPUT_PORT = 5556        # Touch input port
```

Default ports should work fine for most people.

### Change Screen Capture Method

Edit `desktop/src/screen_capture.py` to use different backends:

```python
# Options: 'mss' (fastest), 'pil' (slower), 'cv2' (medium)
capture_method = 'mss'   # Already set to fastest
```

### Change Encoding Quality

Edit `desktop/src/encoder.py`:

```python
# Bitrate for H.264 encoding
BITRATE = '5M'           # 5 Mbps (adjust to 3M, 10M, etc.)

# Keyframe interval
KEYFRAME_INTERVAL = 30   # I-frame every 30 frames
```

---

## Monitoring Server

### While Server is Running

The terminal will show:

```
Frame captured in 51.2ms
H.264 encoded in 2.1ms
Broadcast to 1 client
```

**This is normal!** Screen capture takes the most time.

### Check Performance

```bash
# Terminal 1 - Watch server logs
python3 main.py

# Terminal 2 - Monitor network
netstat -tlnp | grep 555
# or
ss -tlnp | grep 555
```

### Check if Phone Connected

Look for this in server output:
```
Client connected: 192.168.1.100:54321
```

---

## Troubleshooting Server

### Server won't start

**Error: "Address already in use"**
- Port 5555 or 5556 already occupied
- Solution:
  ```bash
  # Kill existing process
  lsof -i :5555
  kill -9 <PID>
  ```

**Error: "Module not found" (mss, opencv, etc.)**
- Dependencies not installed
- Solution:
  ```bash
  pip install -r requirements.txt
  ```

**Error: "Permission denied"**
- You need screen access
- Make sure you're not running as different user
- On Linux: May need `sudo` for screen capture

### Phone can't find server

**Problem: "Cannot connect to 192.168.x.x"**
- Solution 1: Check firewall
  ```bash
  # Linux - Allow ports
  sudo ufw allow 5555/tcp
  sudo ufw allow 5556/tcp
  
  # Or disable firewall temporarily for testing
  sudo ufw disable
  ```
- Solution 2: Check phone is on same WiFi
  - Phone WiFi settings → Same network as computer
- Solution 3: Check server is running
  - Look at terminal - do you see "listening on"?

### No video appears on phone

**Problem: Connected but no video**
- Check server terminal - any errors?
- Try restarting server with:
  ```bash
  python3 main.py
  ```
- Check network speed: `iperf3` or similar tool
- Try USB connection instead (faster)

### Video is choppy/slow

**Problem: Frame rate is low, video stutters**
- Server capture is slow on some systems
- Solution: Check server terminal output
  ```
  Frame captured in 150ms  <- This is too slow!
  ```
- If capture time > 50ms:
  - Reduce screen resolution
  - Close other applications
  - Try different capture method (mss vs pil)

---

## Advanced Usage

### Run Server with Debug Output

```bash
python3 main.py --debug
```

Shows detailed information about every frame.

### Monitor Network Traffic

```bash
# Linux
nethogs

# Or watch bandwidth
iftop
```

### Test Server Without Phone

```bash
# Connect as a test client
python3 -c "
import socket
s = socket.socket()
s.connect(('localhost', 5555))
print('Connected!')
s.recv(1024)
print('Received data!')
"
```

### Performance Profiling

The `desktop/integration_test.py` has performance tests:

```bash
cd desktop
python3 integration_test.py
```

Shows:
- Screen capture time
- Encoding time
- Network speed
- Performance bottlenecks

---

## Network Considerations

### WiFi Connection

**Best Performance:**
- 5 GHz WiFi (faster)
- Close to router
- No interference
- Dedicated connection

**Typical Speed:**
- Bitrate: 5-15 Mbps
- Latency: 30-100ms
- Packet loss: <1%

### USB Connection

**Better Performance:**
- Guaranteed ~50 Mbps
- Lower latency (<20ms)
- No wireless interference

**Setup:**
```bash
# Terminal 1 - Forward ports
adb forward tcp:5555 tcp:5555
adb forward tcp:5556 tcp:5556

# Terminal 2 - Start server
python3 main.py
```

### Low-Bandwidth Mode

If your network is slow:

1. Edit `desktop/src/encoder.py`:
   ```python
   BITRATE = '2M'  # Lower from 5M to 2M
   ```

2. Restart server:
   ```bash
   python3 main.py
   ```

3. Quality will be lower but stream might be smoother

---

## File Structure

```
desktop/
├── main.py                    # Start here
├── requirements.txt          # Python dependencies
├── src/
│   ├── server.py            # Main server (multi-threaded)
│   ├── screen_capture.py    # Screen capture (60 FPS)
│   ├── encoder.py           # H.264 encoding
│   ├── protocol.py          # Message format definition
│   └── __init__.py
├── test.py                  # Unit tests (8 tests)
├── integration_test.py      # Integration tests (3 tests)
└── ...
```

---

## Common Commands

```bash
# Start server
cd ~/fpv-screen-mirror/desktop
python3 main.py

# Stop server (in terminal)
Ctrl+C

# Run tests
python3 test.py

# Run integration tests
python3 integration_test.py

# View server logs
python3 main.py 2>&1 | tee server.log

# Monitor performance
watch -n 1 "ps aux | grep python"
```

---

## Expected Performance

### Typical Values

- Screen capture: 40-60ms (depends on resolution)
- H.264 encoding: 1-5ms (fast)
- Network transmission: 10-30ms (WiFi) / <5ms (USB)
- **Total latency: 50-100ms**

### Frame Rate

- Target: 60 FPS
- Actual: 15-30 FPS typical (network limited)
- Depends on:
  - WiFi bandwidth
  - CPU speed
  - Phone processing power
  - Screen resolution

---

## Using with VR Box

The server outputs stereo format (side-by-side):

```
Desktop Screen: 1920 x 1080

Server Output: 3840 x 1080
                ↓
         ┌──────────┬──────────┐
         │  Left    │  Right   │
         │  Eye     │  Eye     │
         │ 1920x1080│ 1920x1080│
         └──────────┴──────────┘
                ↓
          VR Box Lenses
```

This way, each eye sees the correct perspective for VR!

---

## Next Steps

1. **Test Basic Connection**
   - Start server
   - Open app on phone
   - Verify connection

2. **Test Touch Input**
   - Touch phone screen
   - See your mouse move on desktop

3. **Test VR Box**
   - Insert phone in VR box
   - Look through lenses
   - Try FPV simulator app

4. **Optimize Settings**
   - Adjust bitrate if needed
   - Try different WiFi positions
   - Test USB if WiFi is slow

---

## Support

### Check Logs for Errors

```bash
python3 main.py 2>&1 | tee debug.log
# Then review debug.log
```

### Run Diagnostics

```bash
# Test imports
python3 -c "import mss, cv2; print('OK')"

# Test screen capture
python3 -c "from src.screen_capture import ScreenCapture; print('OK')"

# Test encoder
python3 -c "from src.encoder import H264Encoder; print('OK')"
```

### Review Documentation

- `README.md` - Overall architecture
- `QUICK_START.md` - Quick start guide
- `docs/VR_STEREO_GUIDE.md` - Stereo rendering details

---

## Summary

**To run the server:**

```bash
cd ~/fpv-screen-mirror/desktop
pip install -r requirements.txt    # First time only
python3 main.py
```

**Keep it running while using the app on your phone.**

That's it! The app will connect automatically if on the same WiFi.

---

**Questions?** Check SETUP_AND_RUN.md for complete setup instructions.
