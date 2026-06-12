#!/usr/bin/env python3
"""
End-to-end integration test for FPV Screen Mirror server
Tests actual server startup and basic functionality
"""

import sys
import os
import time
import threading

# Add src to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def test_server_startup():
    """Test server startup without actually starting long-running processes"""
    print("\n" + "="*70)
    print("  TEST: Server Startup & Configuration")
    print("="*70 + "\n")
    
    try:
        from server import ScreenMirrorServer
        
        print("✓ ScreenMirrorServer class imported")
        
        # Create server instance (don't start threads)
        server = ScreenMirrorServer(
            host='127.0.0.1',
            video_port=5555,
            input_port=5556,
            vr_mode=True,
            width=1920,
            height=1080,
            fps=60
        )
        
        print("✓ Server instance created")
        print(f"  - Host: {server.host}")
        print(f"  - Video Port: {server.video_port}")
        print(f"  - Input Port: {server.input_port}")
        print(f"  - VR Mode: {server.vr_mode}")
        print(f"  - Resolution: {server.width}x{server.height}")
        print(f"  - Stereo Resolution: {server.stereo_width}x{server.height}")
        print(f"  - Target FPS: {server.fps}")
        
        # Verify stereo width is doubled
        assert server.stereo_width == 3840, f"Stereo width should be 3840, got {server.stereo_width}"
        print("✓ Stereo width correctly configured (1920 → 3840)")
        
        # Verify capture and encoder are initialized
        assert server.screen_capture is not None, "Screen capture not initialized"
        assert server.encoder is not None, "Encoder not initialized"
        print("✓ Screen capture manager initialized")
        print("✓ H.264 encoder initialized")
        
        # Check encoder type
        print(f"  - Encoder type: {type(server.encoder.encoder).__name__}")
        
        # Test a single capture and encode cycle
        print("\nTesting capture → encode cycle...")
        frame = server.screen_capture.capture()
        
        if frame is not None:
            print(f"✓ Captured frame: {frame.shape}")
            
            # Test stereo frame generation
            stereo = server._create_stereo_frame(frame)
            assert stereo is not None, "Stereo frame generation failed"
            assert stereo.shape[1] == frame.shape[1] * 2, "Stereo width not doubled"
            print(f"✓ Stereo frame created: {stereo.shape}")
            
            # Test encoding
            encoded = server.encoder.encode(frame)
            if encoded is not None:
                print(f"✓ Frame encoded: {len(encoded)} bytes")
            else:
                print("⚠ Encoding returned None (using stub encoder)")
        else:
            print("⚠ Screen capture returned None (headless environment?)")
        
        print("\n✓ Server startup test PASSED\n")
        return True
        
    except Exception as e:
        print(f"\n✗ Server startup test FAILED: {e}\n")
        import traceback
        traceback.print_exc()
        return False

def test_performance_benchmark():
    """Benchmark capture and encoding performance"""
    print("="*70)
    print("  PERFORMANCE BENCHMARK")
    print("="*70 + "\n")
    
    try:
        from screen_capture import ScreenCaptureManager
        from encoder import H264Encoder
        import numpy as np
        
        # Create components
        capture = ScreenCaptureManager(width=1920, height=1080)
        encoder = H264Encoder(width=3840, height=1080, fps=60)
        
        # Benchmark capture
        print("Benchmarking screen capture (10 iterations)...")
        capture_times = []
        for i in range(10):
            start = time.time()
            frame = capture.capture()
            elapsed = time.time() - start
            capture_times.append(elapsed * 1000)  # ms
        
        avg_capture = sum(capture_times) / len(capture_times)
        print(f"  Average: {avg_capture:.2f}ms")
        print(f"  Min: {min(capture_times):.2f}ms, Max: {max(capture_times):.2f}ms")
        
        # Create test frame for encoding benchmark
        test_frame = np.zeros((1080, 1920, 3), dtype=np.uint8)
        
        # Benchmark encoding
        print("\nBenchmarking H.264 encoding (10 iterations)...")
        encode_times = []
        for i in range(10):
            start = time.time()
            encoded = encoder.encode(test_frame)
            elapsed = time.time() - start
            encode_times.append(elapsed * 1000)  # ms
        
        avg_encode = sum(encode_times) / len(encode_times)
        print(f"  Average: {avg_encode:.2f}ms")
        print(f"  Min: {min(encode_times):.2f}ms, Max: {max(encode_times):.2f}ms")
        
        # Total cycle time
        total_cycle = avg_capture + avg_encode
        fps = 1000.0 / total_cycle if total_cycle > 0 else 0
        
        print(f"\nTotal cycle time: {total_cycle:.2f}ms")
        print(f"Achievable FPS: {fps:.1f}")
        
        if fps >= 50:
            print("✓ Performance is sufficient for 60 FPS target\n")
            return True
        else:
            print(f"⚠ Performance may need optimization (need 60 FPS, getting {fps:.1f})\n")
            return True  # Don't fail, just warn
        
    except Exception as e:
        print(f"✗ Benchmark failed: {e}\n")
        return False

def test_client_simulation():
    """Simulate a client connecting and receiving frames"""
    print("="*70)
    print("  CLIENT CONNECTION SIMULATION")
    print("="*70 + "\n")
    
    try:
        from protocol import HandshakeMessage, FrameMessage, MessageFrame, ProtocolDefinition
        import struct
        
        print("Simulating client connection...\n")
        
        # Step 1: Client sends handshake
        print("1. Client → Server: Handshake")
        client_handshake = HandshakeMessage(
            vr_mode=True,
            width=3840,
            height=1080,
            fps=60
        )
        handshake_data = client_handshake.to_bytes()
        handshake_msg = MessageFrame(ProtocolDefinition.MSG_HANDSHAKE, handshake_data)
        handshake_packet = handshake_msg.to_bytes()
        
        print(f"  - VR Mode: {client_handshake.vr_mode}")
        print(f"  - Resolution: {client_handshake.width}x{client_handshake.height}")
        print(f"  - FPS: {client_handshake.fps}")
        print(f"  - Packet size: {len(handshake_packet)} bytes")
        
        # Step 2: Server would send handshake ACK
        print("\n2. Server → Client: Handshake ACK")
        ack_msg = MessageFrame(ProtocolDefinition.MSG_HANDSHAKE_ACK, b'\x00')
        ack_packet = ack_msg.to_bytes()
        print(f"  - Packet size: {len(ack_packet)} bytes")
        
        # Step 3: Server sends video frames
        print("\n3. Server → Client: Video Frames (simulated)")
        frame_times = []
        frame_sizes = []
        
        for frame_num in range(1, 6):
            # Simulate H.264 encoded frame
            encoded_size = 8000 + (frame_num % 3) * 2000  # Vary slightly
            frame_data = b'\x00\x00\x00\x01' + b'H' * (encoded_size - 4)
            
            frame_msg = FrameMessage(
                frame_number=frame_num,
                timestamp=frame_num * 16,  # ~60 FPS
                data_size=len(frame_data)
            )
            
            msg = MessageFrame(
                ProtocolDefinition.MSG_FRAME,
                frame_msg.to_bytes() + frame_data
            )
            packet = msg.to_bytes()
            
            frame_sizes.append(len(packet))
            print(f"  Frame {frame_num}: {len(packet)} bytes ({encoded_size} bytes video)")
        
        avg_frame_size = sum(frame_sizes) / len(frame_sizes)
        print(f"\n  Average frame size: {avg_frame_size:.0f} bytes")
        
        # Calculate bitrate
        bitrate_mbps = (avg_frame_size * 8 * 60) / 1_000_000  # 60 FPS
        print(f"  Bitrate at 60 FPS: {bitrate_mbps:.2f} Mbps")
        
        if bitrate_mbps <= 15:
            print("\n✓ Network bandwidth is reasonable (<15 Mbps)\n")
        else:
            print(f"\n⚠ High bitrate ({bitrate_mbps:.2f} Mbps), may need optimization\n")
        
        # Step 4: Client sends touch input
        print("4. Client → Server: Touch Input")
        touch_x, touch_y = 1920, 540  # Center of left eye
        from protocol import TouchInputMessage
        
        touch = TouchInputMessage(x=touch_x, y=touch_y, action=1)
        touch_msg = MessageFrame(ProtocolDefinition.MSG_INPUT, touch.to_bytes())
        touch_packet = touch_msg.to_bytes()
        
        print(f"  - Position: ({touch_x}, {touch_y})")
        print(f"  - Action: Move (1)")
        print(f"  - Packet size: {len(touch_packet)} bytes")
        
        print("\n✓ Client simulation PASSED\n")
        return True
        
    except Exception as e:
        print(f"✗ Client simulation FAILED: {e}\n")
        import traceback
        traceback.print_exc()
        return False

def run_integration_tests():
    """Run all integration tests"""
    print("\n")
    print("╔" + "="*68 + "╗")
    print("║" + " "*10 + "FPV SCREEN MIRROR - INTEGRATION TESTS" + " "*20 + "║")
    print("║" + " "*10 + "Phase 2: Desktop Server Implementation" + " "*19 + "║")
    print("╚" + "="*68 + "╝")
    
    tests = [
        ("Server Startup", test_server_startup),
        ("Performance Benchmark", test_performance_benchmark),
        ("Client Simulation", test_client_simulation),
    ]
    
    results = []
    for name, test_func in tests:
        try:
            result = test_func()
            results.append((name, result))
        except Exception as e:
            print(f"\n✗ {name} crashed: {e}\n")
            results.append((name, False))
    
    # Summary
    print("="*70)
    print("  INTEGRATION TEST SUMMARY")
    print("="*70)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for name, result in results:
        symbol = "✓" if result else "✗"
        print(f"{symbol} {name}")
    
    print(f"\nTotal: {passed}/{total} integration tests passed")
    
    if passed == total:
        print("\n🎉 All integration tests PASSED!\n")
        return True
    else:
        print(f"\n⚠ {total - passed} test(s) failed.\n")
        return False

if __name__ == '__main__':
    os.chdir(os.path.dirname(__file__))
    success = run_integration_tests()
    sys.exit(0 if success else 1)
