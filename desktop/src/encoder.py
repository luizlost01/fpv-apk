"""
H.264 Video encoder for screen frames
Supports both software and hardware encoding
"""

from abc import ABC, abstractmethod
from typing import Optional
import threading
import time

try:
    import numpy as np
except ImportError:
    np = None


class BaseEncoder(ABC):
    """Base class for video encoders"""
    
    def __init__(self, width: int, height: int, fps: int = 60):
        self.width = width
        self.height = height
        self.fps = fps
        self.frame_count = 0
        self.lock = threading.Lock()
    
    @abstractmethod
    def encode(self, frame: np.ndarray) -> Optional[bytes]:
        """Encode frame to H.264 bitstream"""
        pass
    
    @abstractmethod
    def close(self):
        """Close encoder and cleanup"""
        pass


class H264Encoder(BaseEncoder):
    """H.264 encoder using available backend"""
    
    def __init__(self, width: int, height: int, fps: int = 60):
        super().__init__(width, height, fps)
        self.encoder = None
        self._init_encoder()
    
    def _init_encoder(self):
        """Initialize encoder with available backend"""
        # Try FFmpeg-based encoding first
        try:
            from .encoders.ffmpeg_encoder import FFmpegH264Encoder
            self.encoder = FFmpegH264Encoder(self.width, self.height, self.fps)
            print("Using FFmpeg H.264 encoder")
            return
        except Exception as e:
            print(f"FFmpeg encoder not available: {e}")
        
        # Fallback to OpenCV if available
        try:
            from .encoders.opencv_encoder import OpenCVH264Encoder
            self.encoder = OpenCVH264Encoder(self.width, self.height, self.fps)
            print("Using OpenCV H.264 encoder")
            return
        except Exception as e:
            print(f"OpenCV encoder not available: {e}")
        
        # Use stub encoder
        self.encoder = StubEncoder(self.width, self.height, self.fps)
        print("Using stub encoder (no video output)")
    
    def encode(self, frame: np.ndarray) -> Optional[bytes]:
        """Encode frame"""
        with self.lock:
            self.frame_count += 1
        
        if self.encoder:
            return self.encoder.encode(frame)
        return None
    
    def close(self):
        """Close encoder"""
        if self.encoder:
            self.encoder.close()


class StubEncoder(BaseEncoder):
    """Stub encoder for development (no actual encoding)"""
    
    def encode(self, frame: np.ndarray) -> Optional[bytes]:
        """Return dummy encoded data"""
        # Return a minimal dummy H.264 NAL unit for testing
        return bytes([0x00, 0x00, 0x00, 0x01, 0x65, 0x00, 0x00, 0x00])
    
    def close(self):
        """Nothing to close"""
        pass


# Placeholder for FFmpeg encoder
class FFmpegH264EncoderStub:
    """FFmpeg encoder stub - to be implemented in Phase 2"""
    def __init__(self, width: int, height: int, fps: int):
        self.width = width
        self.height = height
        self.fps = fps
    
    def encode(self, frame) -> Optional[bytes]:
        # TODO: Implement FFmpeg encoding
        return None
    
    def close(self):
        pass


# Placeholder for OpenCV encoder
class OpenCVH264EncoderStub:
    """OpenCV encoder stub - to be implemented in Phase 2"""
    def __init__(self, width: int, height: int, fps: int):
        self.width = width
        self.height = height
        self.fps = fps
    
    def encode(self, frame) -> Optional[bytes]:
        # TODO: Implement OpenCV encoding
        return None
    
    def close(self):
        pass
