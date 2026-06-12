"""
Screen capture manager - cross-platform screen grabbing
Supports Windows, Linux (X11/Wayland), and macOS
"""

import threading
import time
from abc import ABC, abstractmethod
from typing import Optional

try:
    import numpy as np
except ImportError:
    np = None

try:
    from PIL import ImageGrab
except ImportError:
    ImageGrab = None


class ScreenCaptureManager:
    """Cross-platform screen capture"""
    
    def __init__(self, width: int, height: int):
        self.width = width
        self.height = height
        self.is_running = False
        self.last_frame = None
        self.lock = threading.Lock()
        self.platform = self._detect_platform()
        self._init_capture()
    
    def _detect_platform(self) -> str:
        """Detect operating system"""
        import sys
        if sys.platform == 'win32':
            return 'windows'
        elif sys.platform == 'darwin':
            return 'macos'
        else:
            return 'linux'
    
    def _init_capture(self):
        """Initialize platform-specific capture"""
        if self.platform == 'windows' and ImageGrab:
            self.capture_impl = WindowsScreenCapture(self.width, self.height)
        elif self.platform == 'macos' and ImageGrab:
            self.capture_impl = MacOSScreenCapture(self.width, self.height)
        elif self.platform == 'linux':
            self.capture_impl = LinuxScreenCapture(self.width, self.height)
        else:
            # Fallback to PIL
            self.capture_impl = PILScreenCapture(self.width, self.height)
    
    def capture(self) -> Optional[bytes]:
        """Capture current screen frame"""
        try:
            frame = self.capture_impl.capture()
            if frame is not None:
                with self.lock:
                    self.last_frame = frame
            return frame
        except Exception as e:
            print(f"Error capturing screen: {e}")
            return None
    
    def stop(self):
        """Stop capture"""
        self.is_running = False


class BaseScreenCapture(ABC):
    """Base class for screen capture implementations"""
    
    def __init__(self, width: int, height: int):
        self.width = width
        self.height = height
    
    @abstractmethod
    def capture(self) -> Optional[bytes]:
        """Capture screen and return as byte array (BGR format for OpenCV)"""
        pass


class WindowsScreenCapture(BaseScreenCapture):
    """Windows screen capture using PIL"""
    
    def capture(self) -> Optional[bytes]:
        """Capture using PIL ImageGrab"""
        if ImageGrab is None:
            return None
        
        try:
            image = ImageGrab.grab()
            image = image.resize((self.width, self.height))
            
            # Convert to BGR (OpenCV format)
            if np is not None:
                frame = np.array(image)
                # PIL returns RGB, convert to BGR
                frame = frame[:, :, ::-1].copy()
                return frame.astype(np.uint8)
            return None
        except Exception as e:
            print(f"Windows capture error: {e}")
            return None


class MacOSScreenCapture(BaseScreenCapture):
    """macOS screen capture"""
    
    def capture(self) -> Optional[bytes]:
        """Capture using PIL on macOS"""
        if ImageGrab is None:
            return None
        
        try:
            image = ImageGrab.grab()
            image = image.resize((self.width, self.height))
            
            if np is not None:
                frame = np.array(image)
                # PIL returns RGB, convert to BGR
                frame = frame[:, :, ::-1].copy()
                return frame.astype(np.uint8)
            return None
        except Exception as e:
            print(f"macOS capture error: {e}")
            return None


class LinuxScreenCapture(BaseScreenCapture):
    """Linux screen capture (X11 with mss library fallback)"""
    
    def __init__(self, width: int, height: int):
        super().__init__(width, height)
        self.mss_screenshotter = None
        self._init_mss()
    
    def _init_mss(self):
        """Try to initialize mss library for faster capture"""
        try:
            import mss
            self.mss_screenshotter = mss.mss()
        except ImportError:
            pass
    
    def capture(self) -> Optional[bytes]:
        """Capture using mss or PIL fallback"""
        try:
            if self.mss_screenshotter:
                return self._capture_mss()
            else:
                return self._capture_pil()
        except Exception as e:
            print(f"Linux capture error: {e}")
            return None
    
    def _capture_mss(self) -> Optional[bytes]:
        """Capture using mss library (faster)"""
        try:
            monitor = self.mss_screenshotter.monitors[1]  # Primary monitor
            screenshot = self.mss_screenshotter.grab(monitor)
            
            if np is not None:
                from PIL import Image
                frame = np.array(screenshot)[:, :, :3]  # RGB
                # Resize using PIL for better compatibility
                img = Image.fromarray(frame)
                img = img.resize((self.width, self.height))
                frame = np.array(img)
                frame = frame[:, :, ::-1]  # Convert RGB to BGR
                return frame.astype(np.uint8)
            return None
        except Exception as e:
            print(f"MSS capture error: {e}")
            return None
    
    def _capture_pil(self) -> Optional[bytes]:
        """Fallback to PIL"""
        try:
            import subprocess
            import tempfile
            import os
            from PIL import Image
            
            with tempfile.NamedTemporaryFile(suffix='.png', delete=False) as f:
                temp_path = f.name
            
            try:
                subprocess.run(['scrot', temp_path], check=True, timeout=1)
            except:
                try:
                    subprocess.run(['import', '-window', 'root', temp_path], 
                                 check=True, timeout=1)
                except:
                    return None
            
            image = Image.open(temp_path)
            image = image.resize((self.width, self.height))
            
            if np is not None:
                frame = np.array(image)
                if len(frame.shape) == 3 and frame.shape[2] == 3:
                    frame = frame[:, :, ::-1]
                frame = frame.astype(np.uint8)
                os.unlink(temp_path)
                return frame
            
            os.unlink(temp_path)
            return None
        except Exception as e:
            print(f"PIL fallback error: {e}")
            return None


class PILScreenCapture(BaseScreenCapture):
    """Fallback PIL-only screen capture"""
    
    def capture(self) -> Optional[bytes]:
        """Capture using PIL ImageGrab"""
        if ImageGrab is None:
            print("PIL/Pillow not installed. Install with: pip install Pillow")
            return None
        
        try:
            image = ImageGrab.grab()
            image = image.resize((self.width, self.height))
            
            if np is not None:
                frame = np.array(image)
                frame = frame[:, :, ::-1].copy()
                return frame.astype(np.uint8)
            return None
        except Exception as e:
            print(f"PIL capture error: {e}")
            return None
