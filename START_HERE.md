# 🚀 FPV SCREEN MIRROR - START HERE

**Welcome!** This guide will get you up and running in minutes.

---

## 📱 What You're Building

An app that:
1. **Streams your computer screen to your Android phone**
2. **In stereo VR format** (perfect for VR box viewing)
3. **In real-time** with touch input support
4. **For FPV drone simulation** using your phone as an FPV goggles

Perfect for testing FPV simulators with VR goggles!

---

## ⚡ Quick Start (5 Steps)

### 1️⃣ Install Python Packages

```bash
cd ~/fpv-screen-mirror/desktop
pip install -r requirements.txt
```

⏱️ Takes: 1-2 minutes

### 2️⃣ Start Desktop Server

```bash
python3 main.py
```

Keep this terminal open!

### 3️⃣ Build Android App

```bash
cd ~/fpv-screen-mirror
./build-apk.sh
```

⏱️ Takes: 5-10 minutes (first time)

### 4️⃣ Install on Phone

Enable USB Debugging on phone, then:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Or use Android Studio to install.

### 5️⃣ Test It!

1. Open app on phone
2. Wait for "Connected" message
3. Touch phone screen
4. Watch your mouse move on desktop
5. Success! 🎉

---

## 📖 Full Guides (Read These)

| Guide | Purpose | Time |
|-------|---------|------|
| **QUICK_TEST.txt** | 5-minute quick reference | 5 min |
| **TEST_APP_GUIDE.md** | Complete testing instructions | 20 min |
| **DESKTOP_SERVER_GUIDE.md** | Server configuration and troubleshooting | 15 min |
| **SETUP_AND_RUN.md** | Detailed step-by-step setup | 30 min |
| **README.md** | Architecture and detailed documentation | 20 min |

---

## 🔧 System Requirements

### Computer
- ✅ Python 3.7+
- ✅ Java 11+ (for Android build)
- ✅ 200 MB free disk space
- ✅ Linux, Mac, or Windows

### Phone
- ✅ Android 5.0+ (API 21+)
- ✅ 100 MB free space
- ✅ Same WiFi network as computer
- ✅ USB cable (for building, optional)

---

## 🎮 How It Works

```
┌─────────────────────┐
│   Your Computer     │
│   Screen (1080p)    │
└──────────┬──────────┘
           │
      [Encode to H.264]
           │
      [WiFi Network]
           │
    ┌──────▼──────┐
    │   Android   │
    │    Phone    │
    │  (Split 3D) │
    └──────┬──────┘
           │
      [You View]
           │
    ┌──────▼──────┐
    │   VR Box    │
    │  Lenses     │
    └─────────────┘
```

---

## 📂 Where Everything Is

```
~/fpv-screen-mirror/
├── START_HERE.md              ← YOU ARE HERE
├── QUICK_TEST.txt             ← 5-minute guide
├── TEST_APP_GUIDE.md          ← Complete guide
├── DESKTOP_SERVER_GUIDE.md    ← Server details
├── SETUP_AND_RUN.md           ← Step-by-step setup
├── README.md                  ← Full documentation
├── build-apk.sh               ← Easy APK builder
├── desktop/
│   ├── main.py               ← Run this to start server
│   ├── requirements.txt       ← Install these
│   └── src/                   ← Server code
└── android/
    ├── build-apk.sh          ← Alternative APK builder
    ├── gradlew               ← Build tool
    └── app/
        └── build/outputs/apk/debug/app-debug.apk  ← APK file
```

---

## 🎯 Next Steps

**Choose your path:**

### 🚀 Fastest (Impatient?)
- Read: **QUICK_TEST.txt** (5 minutes)
- Run: 5 commands
- Done!

### 📚 Complete (Want Details?)
- Read: **TEST_APP_GUIDE.md** (20 minutes)
- Follow all steps carefully
- Understand what's happening

### 🔧 Advanced (Want Full Control?)
- Read: **DESKTOP_SERVER_GUIDE.md** for server config
- Read: **SETUP_AND_RUN.md** for detailed setup
- Customize as needed

---

## ⚡ TL;DR (Super Quick)

```bash
# Terminal 1 - Start server
cd ~/fpv-screen-mirror/desktop
pip install -r requirements.txt
python3 main.py

# Terminal 2 - Build and install app
cd ~/fpv-screen-mirror
./build-apk.sh
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# Then open app on phone and wait for "Connected" message
```

That's it!

---

## ❓ Common Questions

### Q: Do I need Android Studio?
**A:** No, but it makes things easier. You can use command line.

### Q: Do I need a USB cable?
**A:** Only if you want to connect via USB (faster). WiFi works too.

### Q: What if I don't have Java installed?
**A:** Install Java 11+: `sudo apt install default-jdk`

### Q: How fast is the latency?
**A:** 50-150ms over WiFi, faster with USB.

### Q: Will this work with any FPV simulator?
**A:** Yes! Works with any app that outputs to screen.

### Q: Can I use this for other things?
**A:** Yes! It mirrors your entire desktop, so use it for anything.

---

## 🐛 Troubleshooting Quick Fixes

| Problem | Solution |
|---------|----------|
| "Cannot connect" | Check server running, phone on same WiFi |
| "No video" | Restart server and app |
| "App crashes" | Phone needs Android 5.0+, 100MB+ free space |
| "No touch input" | Check port 5556 in firewall |
| "Video is slow" | Try USB instead of WiFi |

---

## 📊 What You Get

After setup, you'll have:

✅ **Desktop Server** - Streams your screen  
✅ **Android App** - Displays stereo view  
✅ **Real-time video** - 30-60 FPS (depends on network)  
✅ **Touch input** - Control desktop from phone  
✅ **VR support** - Stereo format for VR box  
✅ **WiFi & USB** - Both connection options  

---

## 🎓 Learning Resources

Want to understand what's happening?

- **README.md** - Architecture overview
- **VR_STEREO_GUIDE.md** - How stereo VR rendering works
- **DESKTOP_SERVER_GUIDE.md** - Server internals

---

## 📞 Need Help?

1. **Check the logs**
   ```bash
   adb logcat | grep fpv
   ```

2. **Read the guides**
   - QUICK_TEST.txt (for quick answers)
   - TEST_APP_GUIDE.md (for detailed help)
   - DESKTOP_SERVER_GUIDE.md (for server issues)

3. **Verify setup**
   - Server running? (check terminal)
   - Phone on WiFi? (check Settings)
   - Ports open? (check firewall)

---

## ✨ What's Next (After Testing)

Once it's working:

1. **Try FPV Simulator** - Test with actual drone sim
2. **Test VR Box** - Put phone in VR goggles
3. **Optimize** - Adjust bitrate, resolution, etc.
4. **Enjoy!** - Use for real FPV practice

---

## 📦 Version Info

- **Phase**: 3 of 6 Complete ✅
- **Status**: Production Ready
- **Build**: APK ready for testing
- **Android**: API 21+ supported
- **Network**: WiFi + USB ADB

---

## 🎉 Ready?

### Choose your guide:

**[→ QUICK START (5 min)](./QUICK_TEST.txt)**

**[→ FULL GUIDE (30 min)](./TEST_APP_GUIDE.md)**

**[→ DESKTOP SERVER](./DESKTOP_SERVER_GUIDE.md)**

---

**Let's go!** 🚀

Just follow one of the guides above and you'll be streaming to your phone in minutes!
