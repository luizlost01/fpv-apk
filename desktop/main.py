# FPV Screen Mirror - Desktop Server
# Cross-platform screen capture and video streaming

import sys
import os

# Add src directory to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from server import ScreenMirrorServer

def main():
    """Main entry point for the desktop server"""
    server = ScreenMirrorServer(
        host='0.0.0.0',
        video_port=5554,
        input_port=5556,
        vr_mode=True,
        width=1920,
        height=1080,
        fps=60
    )
    
    try:
        print("Starting FPV Screen Mirror Server...")
        print(f"Video Port: {server.video_port}")
        print(f"Input Port: {server.input_port}")
        print(f"VR Mode: {server.vr_mode}")
        print(f"Resolution: {server.width}x{server.height} @ {server.fps} FPS")
        print("Press Ctrl+C to stop...")
        
        server.start()
    except KeyboardInterrupt:
        print("\nShutting down...")
        server.stop()
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
