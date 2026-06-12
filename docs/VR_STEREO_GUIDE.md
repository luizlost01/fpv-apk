# VR Box Stereo Split-Screen Guide

## Overview
This project is optimized for use in a VR box headset. The screen must be rendered in **side-by-side stereo format** so that each eye sees its own portion of the mirrored screen.

## Screen Layout

```
┌─────────────────────┬─────────────────────┐
│                     │                     │
│    LEFT EYE VIEW    │   RIGHT EYE VIEW    │
│   (Left Half)       │   (Right Half)      │
│                     │                     │
└─────────────────────┴─────────────────────┘

Full Width = 2x Eye Width
Example: 3840 x 1080 (two 1920x1080 halves)
```

## Implementation Details

### Desktop Server
- Capture desktop screen at desired resolution (e.g., 1920x1080)
- **Double the width**: Create stereo frame by placing the capture side-by-side
  - Left half: Original desktop capture
  - Right half: Same desktop capture (duplicate)
- Encode and transmit at 3840x1080 (or proportional resolution)

### Android Client
- Receive 3840x1080 stereo video frames
- Render fullscreen in side-by-side format
- **Important**: Do NOT scale/squeeze - maintain proper aspect ratio per eye

### Touch Input Mapping
When user touches the screen, determine which eye was touched:
- Touch X coordinate 0-1920: Maps to LEFT eye (desktop coords: X)
- Touch X coordinate 1920-3840: Maps to RIGHT eye (desktop coords: X - 1920)

Both eyes see the same content, so either touch position maps to the same desktop coordinate. Send the mapped coordinate back to the desktop server.

## VR Box Placement
1. Insert phone into VR box headset
2. Phone screen displays side-by-side stereo
3. Each eye looks through a lens at one half of the screen
4. Result: Full screen view in 3D (with proper lens distortion in VR box)

## Resolution Guidelines
- **Minimum**: 1920x1080 per eye (3840x2160 total)
- **Recommended**: 1920x1080-2560x1440 per eye
- **Maximum**: Limited by device and network bandwidth

## Performance Notes
- Must maintain 60 FPS for smooth VR experience
- Latency critical: <100ms for responsive FPV simulator
- Higher resolution = higher bandwidth demand

## Testing
1. Without VR box: View side-by-side stereo on screen (you'll see two identical halves)
2. With VR box: Each eye sees one half through proper lens distortion = immersive experience
