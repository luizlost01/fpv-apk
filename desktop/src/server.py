import threading
import socket
import cv2
import numpy as np
from mss import mss

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
        self.screenshot_thread = threading.Thread(target=self._capture_screenshots)
        self.video_server_thread = threading.Thread(target=self._video_server)

    def start(self):
        self.running = True
        self.screenshot_thread.start()
        self.video_server_thread.start()

    def stop(self):
        self.running = False
        self.server_socket.close()
        self.input_socket.close()
        self.screenshot_thread.join()
        self.video_server_thread.join()

    def _capture_screenshots(self):
        with mss() as sct:
            monitor = {"top": 0, "left": 0, "width": self.width, "height": self.height}
            while self.running:
                screenshot = np.array(sct.grab(monitor))
                # Optionally, process the screenshot here
                # For example, apply OpenCV transformations or filters
                # screenshot = cv2.cvtColor(screenshot, cv2.COLOR_RGBA2RGB)

    def _video_server(self):
        while self.running:
            client_socket, addr = self.server_socket.accept()
            print(f"Connected to {addr}")
            
            try:
                cap = cv2.VideoCapture(0)  # Assuming you want to capture from the default camera
                while self.running and cap.isOpened():
                    ret, frame = cap.read()
                    if not ret:
                        break
                    
                    # Encode the frame as JPEG
                    _, buffer = cv2.imencode('.jpg', frame)
                    data = buffer.tobytes()

                    client_socket.sendall(struct.pack("Q", len(data)) + data)  # Send frame size and frame data
                
                cap.release()
            except Exception as e:
                print(f"Video server error: {e}")
            finally:
                client_socket.close()
                print(f"Disconnected from {addr}")

if __name__ == "__main__":
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