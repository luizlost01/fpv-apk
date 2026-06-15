import threading
import socket
import cv2
import numpy as np
from mss import mss
import struct
import time

class ScreenMirrorServer:
    def __init__(self, host, video_port, input_port, vr_mode, width, height, fps):
        self.host = host
        self.video_port = video_port
        self.input_port = input_port
        self.vr_mode = vr_mode
        self.width = width
        self.height = height
        self.fps = fps
        
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.video_port))
        self.server_socket.listen(5)
        
        self.input_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.input_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.input_socket.bind((self.host, self.input_port))
        self.input_socket.listen(5)
        
        self.running = False
        
        # Add a thread-safe variable to hold the latest screen capture
        self.current_frame = None
        self.frame_lock = threading.Lock()
        
        self.screenshot_thread = threading.Thread(target=self._capture_screenshots)
        self.video_server_thread = threading.Thread(target=self._video_server)

    def start(self):
        self.running = True
        self.screenshot_thread.start()
        self.video_server_thread.start()

    def stop(self):
        self.running = False
        # Create dummy connections to unblock the accept() calls
        try:
            socket.create_connection((self.host, self.video_port), timeout=1)
            socket.create_connection((self.host, self.input_port), timeout=1)
        except:
            pass
            
        self.server_socket.close()
        self.input_socket.close()
        self.screenshot_thread.join()
        self.video_server_thread.join()

    def _capture_screenshots(self):
        with mss() as sct:
            monitor = {"top": 0, "left": 0, "width": self.width, "height": self.height}
            while self.running:
                # Grab the screen
                screenshot = np.array(sct.grab(monitor))
                
                # Convert from BGRA (mss format) to BGR (standard OpenCV format)
                frame = cv2.cvtColor(screenshot, cv2.COLOR_BGRA2BGR)
                
                # Safely update the current frame so the video server can read it
                with self.frame_lock:
                    self.current_frame = frame
                
                # Sleep briefly to target the desired FPS and not burn 100% CPU
                time.sleep(1.0 / self.fps)

    def _video_server(self):
        while self.running:
            client_socket, addr = self.server_socket.accept()
            if not self.running:
                break
                
            print(f"Connected to {addr}")
            
            try:
                while self.running:
                    # Safely get the latest frame
                    with self.frame_lock:
                        frame = self.current_frame
                        
                    if frame is None:
                        time.sleep(0.01)
                        continue
                    
                    # Encode the frame as JPEG to send over network
                    ret, buffer = cv2.imencode('.jpg', frame, [int(cv2.IMWRITE_JPEG_QUALITY), 80])
                    if not ret:
                        continue
                        
                    data = buffer.tobytes()

                    # Send frame size (using Q for unsigned long long, 8 bytes) and then the frame data
                    client_socket.sendall(struct.pack("Q", len(data)) + data)
                    
                    # Cap the network sending rate to the target FPS
                    time.sleep(1.0 / self.fps)
                    
            except Exception as e:
                print(f"Video server error or client disconnected: {e}")
            finally:
                client_socket.close()
                print(f"Disconnected from {addr}")

if __name__ == "__main__":
    import sys
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