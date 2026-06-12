# FPV Screen Mirror - Android VR Screen Mirroring App

Mirror your computer screen to an Android device in stereo VR format for drone FPV simulator gameplay in a VR box.

## Overview

**FPV Screen Mirror** is a cross-platform screen mirroring solution optimized for VR box usage. The app captures your desktop screen, encodes it as H.264 video, and streams it in real-time to an Android device where it's displayed in side-by-side stereo format suitable for viewing in a VR box headset.

### Key Features

- 📱 **Real-time Screen Mirroring** - 60 FPS H.264 video streaming
- 🎮 **VR Box Stereo Support** - Side-by-side split screen (left eye | right eye)
- 📡 **Dual Connection** - WiFi (LAN) and USB (ADB) support
- ⚡ **Low Latency** - Optimized for responsive FPV simulator gameplay (<100ms)
- 🖥️ **Cross-Platform** - Windows, Linux (X11/Wayland), and macOS support
- 📲 **Android Native** - Pure Java/Kotlin implementation, API 21+
- 🎯 **Touch Input** - Bidirectional touch input from VR box

## Project Structure

```
fpv-screen-mirror/
├── android/                          # Android App (Kotlin)
│   ├── app/
│   │   ├── build.gradle             # App build config
│   │   ├── AndroidManifest.xml      # Permissions & config
│   │   └── src/main/
│   │       ├── java/com/fpvmirror/
│   │       │   ├── MainActivity.kt               # Main fullscreen activity
│   │       │   ├── StereoSurfaceView.kt         # Custom stereo rendering
│   │       │   ├── network/
│   │       │   │   ├── ProtocolDefinition.kt    # Protocol messages
│   │       │   │   └── NetworkClient.kt         # Network client
│   │       │   └── client/                      # Client components (TBD)
│   │       └── res/                 # Resources (colors, strings, etc)
│   └── build.gradle                 # Top-level build config
│
├── desktop/                          # Desktop Server (Python)
│   ├── main.py                      # Server entry point
│   ├── src/
│   │   ├── server.py                # ScreenMirrorServer main class
│   │   ├── protocol.py              # Binary protocol definitions
│   │   ├── screen_capture.py        # Cross-platform screen capture
│   │   ├── encoder.py               # H.264 video encoder
│   │   └── __init__.py
│   ├── config/                      # Configuration files (TBD)
│   └── requirements.txt             # Python dependencies
│
├── protocol/                         # Shared Protocol Docs (TBD)
│   └── messages.txt                 # Binary message format specs
│
├── docs/
│   ├── README.md                    # This file
│   └── VR_STEREO_GUIDE.md          # VR stereo rendering guide
│
└── .gitignore
```

## Getting Started

### Android App

#### Prerequisites
- Android Studio 2022.1+
- Android SDK 34
- Kotlin 1.8+
- Minimum device: Android 5.0 (API 21)

#### Build
```bash
cd android
./gradlew build
```

#### Run
```bash
./gradlew installDebug
```

### Desktop Server

#### Prerequisites
- Python 3.8+
- FFmpeg or OpenCV (optional, for H.264 encoding)

#### Installation
```bash
cd desktop
pip install -r requirements.txt
```

#### Run
```bash
python main.py
```

By default, the server listens on:
- Video streaming: `0.0.0.0:5555`
- Input events: `0.0.0.0:5556`

## Connection Methods

### WiFi (Recommended for VR Box)
1. Ensure phone and computer are on the same network
2. Find your desktop's IP address:
   - Windows: `ipconfig | findstr "IPv4"`
   - Linux/Mac: `ifconfig | grep "inet "`
3. Update `MainActivity.kt` with server IP: `getServerHostAddress()`
4. Connect via WiFi

### USB (Fallback)
1. Enable ADB on Android device
2. Connect via USB cable
3. Forward ports: `adb forward tcp:5555 tcp:5555`
4. Connect via localhost

## Protocol

### Message Format
```
[Type: 1 byte][Length: 4 bytes][Payload: variable]
```

### Message Types
- `0x01` - Handshake (client → server)
- `0x02` - Handshake ACK (server → client)
- `0x03` - Video Frame (server → client)
- `0x04` - Touch Input (client → server)
- `0x05` - Heartbeat (bidirectional)
- `0x06` - Configuration (server → client)

### Stereo VR Format
- **Resolution**: 3840 x 1080 (side-by-side stereo)
  - Left half: 1920 x 1080 (left eye view)
  - Right half: 1920 x 1080 (right eye view)
  - *Both halves show the same mirrored desktop*

- **Frame Rate**: 60 FPS
- **Codec**: H.264
- **Bitrate**: 5-15 Mbps (adaptive)

## Implementation Status

### Phase 1: Project Setup ✅ (In Progress)
- [x] Android project structure and manifest
- [x] Network protocol definitions
- [x] StereoSurfaceView for rendering
- [x] NetworkClient for WiFi/USB communication
- [x] Desktop server skeleton
- [ ] Project documentation completion

### Phase 2: Desktop Server (Planned)
- [ ] Screen capture (Windows/Linux/macOS)
- [ ] H.264 encoding integration
- [ ] Network server implementation
- [ ] Stereo frame generation

### Phase 3: Android Client (Planned)
- [ ] Video decoder (H.264)
- [ ] Stereo rendering optimization
- [ ] Server discovery (mDNS)
- [ ] Connection UI

### Phase 4-6: Optimization, Testing, Polish (Planned)
- [ ] Latency optimization
- [ ] Performance tuning
- [ ] VR box testing
- [ ] Error handling and reconnection
- [ ] Documentation

## Performance Targets

| Metric | Target |
|--------|--------|
| Frame Rate | 60 FPS |
| Resolution | 3840 x 1080 (per eye: 1920 x 1080) |
| Latency | <100ms end-to-end |
| Bitrate | 5-15 Mbps |
| CPU Usage | <30% on 4-core desktop |
| RAM | <500MB (Android) |

## Configuration

### Server Config (desktop/config/)
- Resolution and FPS
- Video bitrate
- Network ports
- VR stereo mode toggle

### Client Config (Android Settings)
- Server address and port
- Connection type (WiFi/USB)
- VR mode enable/disable
- Resolution preference

## Development

### Code Style
- **Android**: Kotlin (KDoc for public APIs)
- **Desktop**: Python 3 (PEP 257 docstrings)
- **Protocol**: Well-documented binary format

### Testing
- Unit tests for protocol serialization
- Integration tests for network communication
- Manual VR box testing with drone FPV simulators

## Troubleshooting

### "Connection refused" error
- Ensure desktop server is running: `python main.py`
- Check firewall allows ports 5555-5556
- Verify IP address is correct in MainActivity

### Low frame rate or stuttering
- Check network bandwidth with `iperf3`
- Reduce resolution in server config
- Ensure no other apps using the ports

### Video not displaying
- Verify H.264 codec support on device
- Check Android logs: `adb logcat | grep FPVScreenMirror`
- Ensure MediaCodec is available (API 16+)

## Dependencies

### Android
- androidx.appcompat:appcompat
- androidx.constraintlayout:constraintlayout
- androidx.core:core
- com.google.android.material:material
- com.squareup.okhttp3:okhttp
- androidx.media:media
- org.jetbrains.kotlinx:kotlinx-coroutines

### Desktop
- numpy
- Pillow
- (Optional) opencv-python
- (Optional) imageio-ffmpeg

## License

[Add license here]

## Contributing

[Add contribution guidelines]

## Support

For issues, questions, or suggestions:
- GitHub Issues: [Link]
- Documentation: See `/docs` folder
- Troubleshooting: See TROUBLESHOOTING.md

---

**Current Version**: 1.0 (In Development)
**Last Updated**: June 11, 2026
**Status**: Phase 1 - Core Infrastructure Complete
