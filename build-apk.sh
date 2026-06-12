#!/bin/bash
# FPV Screen Mirror - Android APK Builder
# This script builds the APK and provides installation instructions

set -e

echo "════════════════════════════════════════════════════════════"
echo "FPV Screen Mirror - Android APK Builder"
echo "════════════════════════════════════════════════════════════"
echo ""

# Change to android directory
cd "$(dirname "$0")/android"

echo "📱 Building APK..."
echo ""

# Build debug APK
if ! ./gradlew assembleDebug --no-build-cache; then
    echo ""
    echo "❌ Build failed!"
    echo ""
    echo "Troubleshooting:"
    echo "1. Make sure Java 11+ is installed: java -version"
    echo "2. Make sure Android SDK is installed"
    echo "3. Check internet connection (first build downloads dependencies)"
    echo "4. Try: ./gradlew clean build"
    exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo ""
echo "════════════════════════════════════════════════════════════"
echo "✅ BUILD SUCCESSFUL!"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "APK Location: $(pwd)/$APK_PATH"
echo "APK Size: $(ls -lh $APK_PATH | awk '{print $5}')"
echo ""

# Check if ADB is available
if command -v adb &> /dev/null; then
    echo "📲 Android Device Installation Options:"
    echo ""
    echo "Option 1 - Install with ADB (if device connected):"
    echo "  adb install -r $APK_PATH"
    echo ""
    echo "Option 2 - Install with Android Studio:"
    echo "  1. Open Android Studio"
    echo "  2. Click Run → Run 'app'"
    echo "  3. Select your device"
    echo ""
    echo "Option 3 - Manual Installation:"
    echo "  1. Transfer APK to your phone"
    echo "  2. Open file manager and tap the APK"
    echo "  3. Tap Install"
    echo ""
    
    # Try to detect connected devices
    DEVICES=$(adb devices | grep -v "^$" | grep -v "List of attached" | awk '{print $1}')
    
    if [ -z "$DEVICES" ]; then
        echo "⚠️  No connected Android devices detected"
        echo ""
        echo "To connect:"
        echo "  1. Enable USB Debugging on your phone"
        echo "     Settings → Developer Options → USB Debugging"
        echo "  2. Connect phone to computer with USB cable"
        echo "  3. Run: adb devices"
        echo ""
    else
        echo "✅ Connected devices:"
        adb devices | grep -v "^$" | grep -v "List of attached"
        echo ""
        echo "Would you like to install now? Run:"
        echo "  adb install -r $APK_PATH"
        echo ""
    fi
else
    echo "⚠️  ADB not found. Install Android SDK to use automatic installation."
    echo ""
    echo "Manual installation:"
    echo "  1. Connect phone via USB"
    echo "  2. Transfer APK: $APK_PATH"
    echo "  3. Open file manager and install"
    echo ""
fi

echo "════════════════════════════════════════════════════════════"
echo "Next Steps:"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "1. Install APK on your Android phone"
echo ""
echo "2. On your computer, start the desktop server:"
echo "   cd ~/fpv-screen-mirror/desktop"
echo "   python3 main.py"
echo ""
echo "3. On your phone, open the FPV Screen Mirror app"
echo ""
echo "4. App should connect automatically if on same WiFi"
echo ""
echo "For detailed guide, see: ~/fpv-screen-mirror/SETUP_AND_RUN.md"
echo ""
