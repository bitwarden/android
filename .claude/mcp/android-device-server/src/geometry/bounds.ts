/**
 * Geometric primitives for UI element bounds and point operations.
 */

export interface Point {
  x: number;
  y: number;
}

export interface Rect {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export function center(r: Rect): Point {
  return {
    x: Math.floor((r.left + r.right) / 2),
    y: Math.floor((r.top + r.bottom) / 2),
  };
}

export function area(r: Rect): number {
  const w = r.right - r.left;
  const h = r.bottom - r.top;
  return w > 0 && h > 0 ? w * h : 0;
}

export function containsPoint(r: Rect, p: Point): boolean {
  return p.x >= r.left && p.x <= r.right && p.y >= r.top && p.y <= r.bottom;
}

export function overlaps(a: Rect, b: Rect): boolean {
  return !(a.left >= b.right || a.right <= b.left || a.top >= b.bottom || a.bottom <= b.top);
}

export function boundsEqual(a: Rect, b: Rect): boolean {
  return a.left === b.left && a.top === b.top && a.right === b.right && a.bottom === b.bottom;
}

/**
 * Parse Android bounds string "[left,top][right,bottom]" into a Rect.
 */
export function parseBounds(bounds: string): Rect | null {
  const match = bounds.match(/\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]/);
  if (!match) return null;
  return {
    left: parseInt(match[1], 10),
    top: parseInt(match[2], 10),
    right: parseInt(match[3], 10),
    bottom: parseInt(match[4], 10),
  };
}
