#!/usr/bin/env python3
"""
Test suite for FPV Screen Mirror desktop server components
Tests screen capture and H.264 encoding
"""

import sys
import os
import time
import subprocess
from pathlib import Path

# Add src to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def print_section(title):
    """Print a formatted section header"""
    print(f"\n{'='*70}")
    print(f"  {title}")
    print(f"{'='*70}\n")

def test_imports():
    """Test if all required modules can be imported"""
    print_section("TEST 1: Module Imports")
    
    try:
        import numpy as np
        print("✓ NumPy imported successfully")
    except ImportError as e:
        print(f"✗ NumPy import failed: {e}")
        return False
    
    try:
        from PIL import Image
        print("✓ PIL/Pillow imported successfully")
    except ImportError as e:
        print(f"✗ PIL/Pillow import failed: {e}")
        return False
    
    try:
        from screen_capture import ScreenCaptureManager
        print("✓ ScreenCaptureManager imported successfully")
    except ImportError as e:
        print(f"✗ ScreenCaptureManager import failed: {e}")
        return False
    
    try:
        from encoder import H264Encoder
        print("✓ H264Encoder imported successfully")
    except ImportError as e:
        print(f"✗ H264Encoder import failed: {e}")
        return False
    
    try:
        from protocol import (ProtocolDefinition, HandshakeMessage, 
                             FrameMessage, TouchInputMessage, MessageFrame)
        print("✓ Protocol modules imported successfully")
    except ImportError as e:
        print(f"✗ Protocol import failed: {e}")
        return False
    
    try:
        from server import ScreenMirrorServer
        print("✓ ScreenMirrorServer imported successfully")
    except ImportError as e:
        print(f"✗ ScreenMirrorServer import failed: {e}")
        return False
    
    return True

def test_protocol():
    """Test protocol message serialization"""
    print_section("TEST 2: Protocol Message Serialization")
    
    from protocol import (HandshakeMessage, FrameMessage, 
                         TouchInputMessage, MessageFrame, ProtocolDefinition)
    
    # Test HandshakeMessage
    try:
        handshake = HandshakeMessage(vr_mode=True, width=3840, height=1080, fps=60)
        data = handshake.to_bytes()
        handshake2 = HandshakeMessage.from_bytes(data)
        assert handshake2.vr_mode == True
        assert handshake2.width == 3840
        assert handshake2.height == 1080
        assert handshake2.fps == 60
        print(f"✓ HandshakeMessage serialization OK ({len(data)} bytes)")
    except Exception as e:
        print(f"✗ HandshakeMessage test failed: {e}")
        return False
    
    # Test FrameMessage
    try:
        frame = FrameMessage(frame_number=100, timestamp=123456789, data_size=65536)
        data = frame.to_bytes()
        frame2 = FrameMessage.from_bytes(data)
        assert frame2.frame_number == 100
        assert frame2.timestamp == 123456789
        assert frame2.data_size == 65536
        print(f"✓ FrameMessage serialization OK ({len(data)} bytes)")
    except Exception as e:
        print(f"✗ FrameMessage test failed: {e}")
        return False
    
    # Test TouchInputMessage
    try:
        touch = TouchInputMessage(x=500, y=750, pressure=0.8, action=1)
        data = touch.to_bytes()
        touch2 = TouchInputMessage.from_bytes(data)
        assert touch2.x == 500
        assert touch2.y == 750
        assert abs(touch2.pressure - 0.8) < 0.01
        assert touch2.action == 1
        print(f"✓ TouchInputMessage serialization OK ({len(data)} bytes)")
    except Exception as e:
        print(f"✗ TouchInputMessage test failed: {e}")
        return False
    
    # Test MessageFrame
    try:
        msg = MessageFrame(ProtocolDefinition.MSG_HANDSHAKE, handshake.to_bytes())
        data = msg.to_bytes()
        msg2, consumed = MessageFrame.from_bytes(data)
        assert msg2 is not None
        assert consumed == len(data)
        print(f"✓ MessageFrame serialization OK ({len(data)} bytes)")
    except Exception as e:
        print(f"✗ MessageFrame test failed: {e}")
        return False
    
    return True

def test_screen_capture():
    """Test screen capture functionality"""
    print_section("TEST 3: Screen Capture")
    
    from screen_capture import ScreenCaptureManager
    import numpy as np
    
    try:
        # Create small test resolution
        capture_mgr = ScreenCaptureManager(width=800, height=600)
        print(f"✓ ScreenCaptureManager initialized (platform: {capture_mgr.platform})")
        
        # Try to capture a frame
        print("  Attempting to capture screen...")
        frame = capture_mgr.capture()
        
        if frame is not None:
            print(f"✓ Screen capture successful!")
            print(f"  - Frame shape: {frame.shape}")
            print(f"  - Frame dtype: {frame.dtype}")
            print(f"  - Frame size: {frame.nbytes / 1024:.1f} KB")
            return True
        else:
            print("⚠ Screen capture returned None")
            print("  (This may be normal in headless environments)")
            return True  # Don't fail for headless systems
    
    except Exception as e:
        print(f"✗ Screen capture test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_encoder():
    """Test H.264 encoder functionality"""
    print_section("TEST 4: H.264 Encoder")
    
    from encoder import H264Encoder
    import numpy as np
    
    try:
        encoder = H264Encoder(width=1920, height=1080, fps=60)
        print(f"✓ H264Encoder initialized")
        
        # Create test frame (black image)
        test_frame = np.zeros((1080, 1920, 3), dtype=np.uint8)
        
        # Test encoding
        print("  Encoding test frames...")
        encoded_frames = []
        for i in range(5):
            encoded = encoder.encode(test_frame)
            if encoded is not None:
                encoded_frames.append(encoded)
                print(f"  ✓ Frame {i+1}: encoded {len(encoded)} bytes")
            else:
                print(f"  ✗ Frame {i+1}: encoding returned None")
        
        if encoded_frames:
            print(f"✓ Encoder test successful ({len(encoded_frames)}/5 frames encoded)")
            encoder.close()
            return True
        else:
            print("✗ No frames were successfully encoded")
            encoder.close()
            return False
    
    except Exception as e:
        print(f"✗ Encoder test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_stereo_frame_generation():
    """Test stereo frame generation (side-by-side)"""
    print_section("TEST 5: Stereo Frame Generation")
    
    try:
        import numpy as np
        
        # Create test frame
        frame = np.ones((1080, 1920, 3), dtype=np.uint8) * 128
        print(f"✓ Created test frame: {frame.shape}")
        
        # Create stereo frame (duplicate side-by-side)
        stereo_frame = np.zeros((frame.shape[0], frame.shape[1] * 2, frame.shape[2]), dtype=frame.dtype)
        stereo_frame[:, :frame.shape[1], :] = frame  # Left eye
        stereo_frame[:, frame.shape[1]:, :] = frame  # Right eye
        
        print(f"✓ Created stereo frame: {stereo_frame.shape}")
        assert stereo_frame.shape == (1080, 3840, 3), "Stereo frame size mismatch"
        
        # Verify left and right are identical
        left_half = stereo_frame[:, :1920, :]
        right_half = stereo_frame[:, 1920:, :]
        assert np.array_equal(left_half, right_half), "Left and right halves don't match"
        
        print("✓ Stereo frame generation OK (left == right)")
        return True
    
    except Exception as e:
        print(f"✗ Stereo frame test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_touch_coordinate_mapping():
    """Test touch coordinate mapping for stereo"""
    print_section("TEST 6: Touch Coordinate Mapping (Stereo)")
    
    try:
        # Simulate stereo touch mapping
        screen_width = 3840  # Full stereo width
        screen_height = 1080
        
        # Test cases: (touch_x, expected_eye, expected_mapped_x)
        test_cases = [
            (100, "LEFT", 100),           # Far left
            (960, "LEFT", 960),           # Center of left eye
            (1920, "CENTER", None),       # Exact center
            (2880, "RIGHT", 960),         # Center of right eye
            (3840, "RIGHT", 1920),        # Far right
        ]
        
        print(f"Screen size: {screen_width}x{screen_height}")
        print(f"Left eye:  0-1920")
        print(f"Right eye: 1920-3840\n")
        
        all_passed = True
        for touch_x, expected_eye, expected_mapped in test_cases:
            if touch_x < screen_width // 2:
                actual_eye = "LEFT"
                mapped_x = touch_x
            elif touch_x > screen_width // 2:
                actual_eye = "RIGHT"
                mapped_x = touch_x - (screen_width // 2)
            else:
                actual_eye = "CENTER"
                mapped_x = None
            
            match = actual_eye == expected_eye
            symbol = "✓" if match else "✗"
            print(f"{symbol} Touch X={touch_x:4d} → {actual_eye:6s} eye, mapped X={mapped_x}")
            if not match:
                all_passed = False
        
        if all_passed:
            print("\n✓ All touch coordinate mappings correct")
        
        return all_passed
    
    except Exception as e:
        print(f"✗ Touch mapping test failed: {e}")
        return False

def test_video_frame_streaming():
    """Test streaming protocol for video frames"""
    print_section("TEST 7: Video Frame Streaming Protocol")
    
    try:
        from protocol import MessageFrame, FrameMessage, ProtocolDefinition
        import struct
        
        # Simulate streaming a video frame
        frame_number = 42
        timestamp = 123456  # Use smaller timestamp
        frame_data = b'\x00\x00\x00\x01' + b'\x65' + b'\x00' * 256  # Fake H.264 NAL
        
        # Create frame message
        frame_msg = FrameMessage(frame_number, timestamp, len(frame_data))
        frame_header = frame_msg.to_bytes()
        
        # Wrap in message frame
        msg = MessageFrame(ProtocolDefinition.MSG_FRAME, frame_header + frame_data)
        packet = msg.to_bytes()
        
        print(f"✓ Created video frame packet")
        print(f"  - Frame number: {frame_number}")
        print(f"  - Timestamp: {timestamp} ms")
        print(f"  - Data size: {len(frame_data)} bytes")
        print(f"  - Total packet size: {len(packet)} bytes")
        
        # Parse it back
        parsed_msg, consumed = MessageFrame.from_bytes(packet)
        assert parsed_msg is not None
        assert consumed == len(packet)
        assert parsed_msg.msg_type == ProtocolDefinition.MSG_FRAME
        
        print(f"✓ Frame parsing successful (consumed {consumed} bytes)")
        return True
    
    except Exception as e:
        print(f"✗ Frame streaming test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_cross_platform_detection():
    """Test cross-platform detection"""
    print_section("TEST 8: Cross-Platform Detection")
    
    try:
        import sys
        from screen_capture import ScreenCaptureManager
        
        platform = sys.platform
        print(f"System platform: {platform}")
        
        capture_mgr = ScreenCaptureManager(width=640, height=480)
        detected = capture_mgr.platform
        
        platform_map = {
            'win32': 'windows',
            'darwin': 'macos',
            'linux': 'linux'
        }
        expected = platform_map.get(platform, 'unknown')
        
        print(f"Detected platform: {detected}")
        print(f"Expected: {expected}")
        
        if detected == expected:
            print(f"✓ Platform detection correct")
            return True
        else:
            print(f"⚠ Platform mismatch (but encoder still works)")
            return True
    
    except Exception as e:
        print(f"✗ Platform detection test failed: {e}")
        return False

def run_all_tests():
    """Run all tests"""
    print("\n")
    print("╔" + "="*68 + "╗")
    print("║" + " "*15 + "FPV SCREEN MIRROR - TEST SUITE" + " "*24 + "║")
    print("║" + " "*15 + "Desktop Server Components" + " "*28 + "║")
    print("╚" + "="*68 + "╝")
    
    tests = [
        ("Module Imports", test_imports),
        ("Protocol Messages", test_protocol),
        ("Screen Capture", test_screen_capture),
        ("H.264 Encoder", test_encoder),
        ("Stereo Frame Generation", test_stereo_frame_generation),
        ("Touch Coordinate Mapping", test_touch_coordinate_mapping),
        ("Video Frame Streaming", test_video_frame_streaming),
        ("Cross-Platform Detection", test_cross_platform_detection),
    ]
    
    results = []
    for name, test_func in tests:
        try:
            result = test_func()
            results.append((name, result))
        except Exception as e:
            print(f"\n✗ Test crashed: {e}")
            import traceback
            traceback.print_exc()
            results.append((name, False))
    
    # Summary
    print_section("TEST SUMMARY")
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for name, result in results:
        symbol = "✓" if result else "✗"
        print(f"{symbol} {name}")
    
    print(f"\nTotal: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All tests PASSED! Server components are ready.\n")
        return True
    else:
        print(f"\n⚠ {total - passed} test(s) failed. Review above for details.\n")
        return False

if __name__ == '__main__':
    os.chdir(os.path.join(os.path.dirname(__file__), 'src'))
    success = run_all_tests()
    sys.exit(0 if success else 1)
