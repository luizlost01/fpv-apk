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
        # Low latency socket options
        self.server_socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.server_socket.bind((self.host, self.video_port))
        self.server_socket.listen(5)

        self.running = False
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
        self.server_socket.close()

    def _capture_screenshots(self):
        with mss() as sct:
            monitor = {"top": 0, "left": 0, "width": self.width, "height": self.height}
            target_frame_time = 1.0 / self.fps
            while self.running:
                start_time = time.time()
                screenshot = np.array(sct.grab(monitor))
                frame = cv2.cvtColor(screenshot, cv2.COLOR_BGRA2BGR)
                
                with self.frame_lock:
                    self.current_frame = frame
                
                # Dynamic sleep to maintain target FPS
                elapsed = time.time() - start_time
                sleep_time = max(0, target_frame_time - elapsed)
                time.sleep(sleep_time)

    def _video_server(self):
        while self.running:
            try:
                client_socket, addr = self.server_socket.accept()
                client_socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                client_socket.settimeout(5.0)
                print(f"Connected to {addr}")

                last_frame_sent = None

                while self.running:
                    with self.frame_lock:
                        frame = self.current_frame

                    if frame is None or frame is last_frame_sent:
                        time.sleep(0.001)
                        continue
                    
                    # Faster JPEG encoding (Lower quality = much faster + less bandwidth)
                    ret, buffer = cv2.imencode('.jpg', frame, [int(cv2.IMWRITE_JPEG_QUALITY), 50])
                    if not ret:
                        continue
                        
                    data = buffer.tobytes()
                    try:
                        # Send 8-byte length + payload
                        client_socket.sendall(struct.pack("<Q", len(data)) + data)
                        last_frame_sent = frame
                    except (socket.error, BrokenPipeError):
                        break

            except Exception as e:
                print(f"Server error: {e}")
            finally:
                print("Client disconnected")

if __name__ == "__main__":
    server = ScreenMirrorServer('0.0.0.0', 5554, 5556, True, 1920, 1080, 60)
    server.start()
