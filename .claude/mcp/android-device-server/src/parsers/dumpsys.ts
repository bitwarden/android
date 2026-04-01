/**
 * Structured parser for `adb shell dumpsys window windows` output.
 *
 * Extracts window name, type, surface visibility, and touchable region
 * from the multi-line per-window blocks. Replaces the fragile awk
 * state machine from the shell scripts.
 */

import { type Rect, type Point, containsPoint } from '../geometry/bounds.js';

export interface WindowInfo {
  name: string;
  type: string;
  hasSurface: boolean;
  touchableRegion: Rect | null;
}

/**
 * Parse `dumpsys window windows` output into structured window objects.
 */
export function parseDumpsysWindows(output: string): WindowInfo[] {
  const windows: WindowInfo[] = [];
  let current: Partial<WindowInfo> | null = null;

  for (const line of output.split('\n')) {
    // New window block: "  Window #N Window{hash u0 NAME}:"
    const windowMatch = line.match(/Window #\d+ Window\{[0-9a-f]+ \S+ (.+)\}:/);
    if (windowMatch) {
      if (current?.name) {
        windows.push(finalizeWindow(current));
      }
      current = { name: windowMatch[1], type: '', hasSurface: false, touchableRegion: null };
      continue;
    }

    if (!current) continue;

    // Window type: " ty=TYPE " (leading space to avoid matching mViewVisibility=0x0)
    // Only match on the mAttrs line, not ROTATION_ lines
    if (!current.type && line.includes('mAttrs=') && line.includes(' ty=')) {
      const tyMatch = line.match(/ ty=(\S+)/);
      if (tyMatch) {
        current.type = tyMatch[1];
      }
    }

    // Surface visibility
    if (line.includes('mHasSurface=true')) {
      current.hasSurface = true;
    }

    // Touchable region: SkRegion((l,t,r,b)) or SkRegion((l,t,r,b)(l2,t2,r2,b2))
    // We take the first rect if multiple. Empty SkRegion() means no touchable area.
    if (line.includes('touchable region=SkRegion(')) {
      const regionMatch = line.match(/SkRegion\(\((\d+),(\d+),(\d+),(\d+)\)/);
      if (regionMatch) {
        current.touchableRegion = {
          left: parseInt(regionMatch[1], 10),
          top: parseInt(regionMatch[2], 10),
          right: parseInt(regionMatch[3], 10),
          bottom: parseInt(regionMatch[4], 10),
        };
      }
      // SkRegion() with no coords = no touchable area, leave as null
    }
  }

  // Don't forget the last window
  if (current?.name) {
    windows.push(finalizeWindow(current));
  }

  return windows;
}

function finalizeWindow(partial: Partial<WindowInfo>): WindowInfo {
  return {
    name: partial.name ?? '',
    type: partial.type ?? '',
    hasSurface: partial.hasSurface ?? false,
    touchableRegion: partial.touchableRegion ?? null,
  };
}

/**
 * Find the first overlay window whose touchable region contains the given point.
 *
 * Filters out BASE_APPLICATION windows (the app itself) and windows without
 * a visible surface or touchable region. Only windows that actually intercept
 * taps are considered.
 */
export function findOverlayAtPoint(windows: WindowInfo[], point: Point): WindowInfo | null {
  for (const win of windows) {
    if (
      win.hasSurface &&
      win.type !== 'BASE_APPLICATION' &&
      win.type !== '' &&
      win.touchableRegion &&
      containsPoint(win.touchableRegion, point)
    ) {
      return win;
    }
  }
  return null;
}
