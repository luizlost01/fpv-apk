"""
Core server implementation for FPV Screen Mirror
Handles network communication and video streaming
"""

import socket
import threading
import time
import struct
from typing import Optional, Callable

try:
    from protocol import ProtocolDefinition, HandshakeMessage, FrameMessage
    from screen_capture import ScreenCaptureManager
    from encoder import H264Encoder
except ImportError:
    from .protocol import ProtocolDefinition, HandshakeMessage, FrameMessage
    from .screen_capture import ScreenCaptureManager
    from .encoder import H264Encoder

class ScreenMirrorServer:
    def __init__(
        self,
        host: str = '0.0.0.0',
        video_port: int = 5555,
        input_port: int = 5556,
        vr_mode: bool = True,
        width: int = 1920,
        height: int = 1080,
        fps: int = 60
    ):
        self.host = host
        self.video_port = video_port
        self.input_port = input_port
        self.vr_mode = vr_mode
        self.width = width
        self.height = height
        self.fps = fps
        
        # Stereo width for VR (double the width)
        self.stereo_width = width * 2 if vr_mode else width
        
        self.is_running = False
        self.clients = []
        self.lock = threading.Lock()
        
        # Initialize components
        self.screen_capture = ScreenCaptureManager(width, height)
        self.encoder = H264Encoder(self.stereo_width, height, fps)
        
        # Threads
        self.capture_thread: Optional[threading.Thread] = None
        self.video_server_thread: Optional[threading.Thread] = None
        self.input_server_thread: Optional[threading.Thread] = None
    
    def start(self):
        """Start the server"""
        self.is_running = True
        
        # Start capture thread
        self.capture_thread = threading.Thread(target=self._capture_loop, daemon=True)
        self.capture_thread.start()
        
        # Start network servers
        self.video_server_thread = threading.Thread(target=self._video_server, daemon=True)
        self.video_server_thread.start()
        
        self.input_server_thread = threading.Thread(target=self._input_server, daemon=True)
        self.input_server_thread.start()
        
        # Keep main thread alive
        try:
            while self.is_running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()
    
    def stop(self):
        """Stop the server"""
        self.is_running = False
        self.screen_capture.stop()
        
        # Close all client connections
        with self.lock:
            for client in self.clients:
                try:
                    client['socket'].close()
                except:
                    pass
            self.clients.clear()
    
    def _capture_loop(self):
        """Main capture loop - captures screen and encodes"""
        frame_time = 1.0 / self.fps
        
        while self.is_running:
            start_time = time.time()
            
            try:
                # Capture screen
                frame = self.screen_capture.capture()
                
                if frame is not None and self.vr_mode:
                    # Create stereo frame (duplicate for side-by-side VR)
                    frame = self._create_stereo_frame(frame)
                
                if frame is not None:
                    # Encode frame
                    encoded_data = self.encoder.encode(frame)
                    
                    if encoded_data is not None:
                        # Broadcast to all connected clients
                        self._broadcast_frame(encoded_data)
                
            except Exception as e:
                print(f"Error in capture loop: {e}")
            
            # Maintain frame rate
            elapsed = time.time() - start_time
            sleep_time = frame_time - elapsed
            if sleep_time > 0:
                time.sleep(sleep_time)
    
    def _create_stereo_frame(self, frame):
        """Create stereo frame by duplicating (side-by-side)"""
        import numpy as np
        # Frame is HxWx3 (BGR or RGB)
        # Create stereo by placing side-by-side
        stereo_frame = np.zeros((frame.shape[0], frame.shape[1] * 2, frame.shape[2]), dtype=frame.dtype)
        stereo_frame[:, :frame.shape[1], :] = frame  # Left eye
        stereo_frame[:, frame.shape[1]:, :] = frame  # Right eye
        return stereo_frame
    
    def _broadcast_frame(self, frame_data: bytes):
        """Broadcast frame to all connected clients"""
        with self.lock:
            disconnected = []
            for i, client in enumerate(self.clients):
                try:
                    # Send frame message
                    client['socket'].sendall(frame_data)
                except Exception as e:
                    print(f"Error sending to client: {e}")
                    disconnected.append(i)
            
            # Remove disconnected clients
            for i in reversed(disconnected):
                self.clients.pop(i)
    
    def _video_server(self):
        """Video streaming server"""
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            server_socket.bind((self.host, self.video_port))
            server_socket.listen(5)
            print(f"Video server listening on {self.host}:{self.video_port}")
            
            while self.is_running:
                try:
                    client_socket, client_addr = server_socket.accept()
                    print(f"Client connected: {client_addr}")
                    
                    with self.lock:
                        self.clients.append({
                            'socket': client_socket,
                            'addr': client_addr,
                            'connected_time': time.time()
                        })
                    
                    # Handle handshake in a separate thread
                    thread = threading.Thread(
                        target=self._handle_client_handshake,
                        args=(client_socket, client_addr),
                        daemon=True
                    )
                    thread.start()
                    
                except Exception as e:
                    print(f"Error accepting connection: {e}")
        
        finally:
            server_socket.close()
    
    def _handle_client_handshake(self, client_socket: socket.socket, client_addr):
        """Handle client handshake"""
        try:
            # TODO: Implement proper handshake protocol
            pass
        except Exception as e:
            print(f"Error in handshake: {e}")
        finally:
            client_socket.close()
    
    def _input_server(self):
        """Input event receiving server"""
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            server_socket.bind((self.host, self.input_port))
            server_socket.listen(5)
            print(f"Input server listening on {self.host}:{self.input_port}")
            
            while self.is_running:
                try:
                    client_socket, client_addr = server_socket.accept()
                    print(f"Input client connected: {client_addr}")
                    
                    # Handle input in a separate thread
                    thread = threading.Thread(
                        target=self._handle_input,
                        args=(client_socket, client_addr),
                        daemon=True
                    )
                    thread.start()
                    
                except Exception as e:
                    print(f"Error accepting input connection: {e}")
        
        finally:
            server_socket.close()
    
    def _handle_input(self, client_socket: socket.socket, client_addr):
        """Handle input events from client"""
        try:
            while self.is_running:
                data = client_socket.recv(1024)
                if not data:
                    break
                # TODO: Process input events
        except Exception as e:
            print(f"Error handling input: {e}")
        finally:
            client_socket.close()
