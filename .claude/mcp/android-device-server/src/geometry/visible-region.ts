/**
 * Visible region computation for partially obstructed UI elements.
 *
 * When a target element is partially covered by an obstructor (FAB, PiP, dialog),
 * this module finds the largest unobstructed rectangular strip and returns its
 * center as an alternative tap point.
 */

import { type Rect, type Point, area, center } from './bounds.js';

export interface VisibleStrip {
  rect: Rect;
  center: Point;
  area: number;
}

/**
 * Find the largest visible rectangular strip of the target not covered by the obstructor.
 *
 * Evaluates 4 candidate strips:
 * - Top: above the obstructor, full target width
 * - Bottom: below the obstructor, full target width
 * - Left: left of the obstructor, full target height
 * - Right: right of the obstructor, full target height
 *
 * Returns the strip with the largest area, or null if fully obscured.
 */
export function largestVisibleStrip(target: Rect, obstructor: Rect): VisibleStrip | null {
  const candidates: Rect[] = [];

  // Top strip: above obstructor, full target width
  if (obstructor.top > target.top) {
    candidates.push({
      left: target.left,
      top: target.top,
      right: target.right,
      bottom: obstructor.top,
    });
  }

  // Bottom strip: below obstructor, full target width
  if (obstructor.bottom < target.bottom) {
    candidates.push({
      left: target.left,
      top: obstructor.bottom,
      right: target.right,
      bottom: target.bottom,
    });
  }

  // Left strip: left of obstructor, full target height
  if (obstructor.left > target.left) {
    candidates.push({
      left: target.left,
      top: target.top,
      right: obstructor.left,
      bottom: target.bottom,
    });
  }

  // Right strip: right of obstructor, full target height
  if (obstructor.right < target.right) {
    candidates.push({
      left: obstructor.right,
      top: target.top,
      right: target.right,
      bottom: target.bottom,
    });
  }

  if (candidates.length === 0) return null;

  let best: Rect = candidates[0];
  let bestArea = area(candidates[0]);

  for (let i = 1; i < candidates.length; i++) {
    const a = area(candidates[i]);
    if (a > bestArea) {
      best = candidates[i];
      bestArea = a;
    }
  }

  if (bestArea <= 0) return null;

  return {
    rect: best,
    center: center(best),
    area: bestArea,
  };
}
