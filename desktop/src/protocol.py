"""
Protocol definitions for FPV Screen Mirror
Binary message format for efficient communication
"""

import struct
from typing import Tuple

class ProtocolDefinition:
    """Protocol constants"""
    MSG_HANDSHAKE = 0x01
    MSG_HANDSHAKE_ACK = 0x02
    MSG_FRAME = 0x03
    MSG_INPUT = 0x04
    MSG_HEARTBEAT = 0x05
    MSG_CONFIG = 0x06
    
    DEFAULT_VIDEO_PORT = 5555
    DEFAULT_INPUT_PORT = 5556
    HEARTBEAT_INTERVAL_MS = 1000
    FRAME_TIMEOUT_MS = 5000


class HandshakeMessage:
    """Handshake message format"""
    FORMAT = '>BBBB HHI'  # version_major, version_minor, vr_mode, reserved, width, height, fps
    SIZE = struct.calcsize(FORMAT)
    
    def __init__(self, version_major: int = 1, version_minor: int = 0, 
                 vr_mode: bool = True, width: int = 3840, 
                 height: int = 1080, fps: int = 60):
        self.version_major = version_major
        self.version_minor = version_minor
        self.vr_mode = vr_mode
        self.width = width
        self.height = height
        self.fps = fps
    
    def to_bytes(self) -> bytes:
        """Serialize to bytes"""
        return struct.pack(
            self.FORMAT,
            self.version_major,
            self.version_minor,
            1 if self.vr_mode else 0,
            0,  # reserved
            self.width,
            self.height,
            self.fps
        )
    
    @classmethod
    def from_bytes(cls, data: bytes) -> 'HandshakeMessage':
        """Deserialize from bytes"""
        if len(data) < cls.SIZE:
            raise ValueError("Invalid handshake message size")
        vmaj, vmin, vr, _, w, h, fps = struct.unpack(cls.FORMAT, data[:cls.SIZE])
        return cls(vmaj, vmin, bool(vr), w, h, fps)


class FrameMessage:
    """Frame message header (actual frame data follows)"""
    FORMAT = '>I I I'  # frame_number, timestamp, data_size
    SIZE = struct.calcsize(FORMAT)
    
    def __init__(self, frame_number: int, timestamp: int, data_size: int):
        self.frame_number = frame_number
        self.timestamp = timestamp
        self.data_size = data_size
    
    def to_bytes(self) -> bytes:
        """Serialize to bytes"""
        return struct.pack(
            self.FORMAT,
            self.frame_number,
            self.timestamp,
            self.data_size
        )
    
    @classmethod
    def from_bytes(cls, data: bytes) -> 'FrameMessage':
        """Deserialize from bytes"""
        if len(data) < cls.SIZE:
            raise ValueError("Invalid frame message size")
        frame_num, ts, size = struct.unpack(cls.FORMAT, data[:cls.SIZE])
        return cls(frame_num, ts, size)


class TouchInputMessage:
    """Touch input message"""
    FORMAT = '>i i f B'  # x, y, pressure, action
    SIZE = struct.calcsize(FORMAT)
    
    ACTION_DOWN = 0
    ACTION_MOVE = 1
    ACTION_UP = 2
    
    def __init__(self, x: int, y: int, pressure: float = 1.0, action: int = 0):
        self.x = x
        self.y = y
        self.pressure = pressure
        self.action = action
    
    def to_bytes(self) -> bytes:
        """Serialize to bytes"""
        return struct.pack(
            self.FORMAT,
            self.x,
            self.y,
            self.pressure,
            self.action
        )
    
    @classmethod
    def from_bytes(cls, data: bytes) -> 'TouchInputMessage':
        """Deserialize from bytes"""
        if len(data) < cls.SIZE:
            raise ValueError("Invalid touch message size")
        x, y, pressure, action = struct.unpack(cls.FORMAT, data[:cls.SIZE])
        return cls(x, y, pressure, action)


class MessageFrame:
    """Wrapper for protocol messages with type and length"""
    HEADER_SIZE = 5  # 1 byte type + 4 bytes length
    
    def __init__(self, msg_type: int, payload: bytes):
        self.msg_type = msg_type
        self.payload = payload
    
    def to_bytes(self) -> bytes:
        """Serialize complete message"""
        length = len(self.payload)
        return struct.pack('>B I', self.msg_type, length) + self.payload
    
    @classmethod
    def from_bytes(cls, data: bytes) -> Tuple['MessageFrame', int]:
        """
        Deserialize from bytes
        Returns (MessageFrame, bytes_consumed)
        """
        if len(data) < cls.HEADER_SIZE:
            return None, 0
        
        msg_type, length = struct.unpack('>B I', data[:cls.HEADER_SIZE])
        total_size = cls.HEADER_SIZE + length
        
        if len(data) < total_size:
            return None, 0
        
        payload = data[cls.HEADER_SIZE:total_size]
        return cls(msg_type, payload), total_size
