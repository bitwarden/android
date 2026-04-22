import { describe, it, expect } from 'vitest';
import {
  center,
  area,
  containsPoint,
  overlaps,
  boundsEqual,
  parseBounds,
  type Rect,
} from './bounds.js';
import { largestVisibleStrip } from './visible-region.js';
import { detectObstruction } from './obstruction.js';
import type { UiNode } from '../parsers/xml.js';
import type { WindowInfo } from '../parsers/dumpsys.js';

describe('bounds', () => {
  const rect: Rect = { left: 100, top: 200, right: 500, bottom: 600 };

  describe('center', () => {
    it('returns the center point of a rect', () => {
      expect(center(rect)).toEqual({ x: 300, y: 400 });
    });

    it('floors fractional centers', () => {
      expect(center({ left: 0, top: 0, right: 101, bottom: 101 })).toEqual({ x: 50, y: 50 });
    });
  });

  describe('area', () => {
    it('computes area of a valid rect', () => {
      expect(area(rect)).toBe(400 * 400);
    });

    it('returns 0 for zero-width rect', () => {
      expect(area({ left: 100, top: 200, right: 100, bottom: 600 })).toBe(0);
    });

    it('returns 0 for inverted rect', () => {
      expect(area({ left: 500, top: 200, right: 100, bottom: 600 })).toBe(0);
    });
  });

  describe('containsPoint', () => {
    it('returns true for point inside rect', () => {
      expect(containsPoint(rect, { x: 300, y: 400 })).toBe(true);
    });

    it('returns true for point on edge', () => {
      expect(containsPoint(rect, { x: 100, y: 200 })).toBe(true);
      expect(containsPoint(rect, { x: 500, y: 600 })).toBe(true);
    });

    it('returns false for point outside rect', () => {
      expect(containsPoint(rect, { x: 50, y: 400 })).toBe(false);
      expect(containsPoint(rect, { x: 300, y: 700 })).toBe(false);
    });
  });

  describe('overlaps', () => {
    it('returns true for overlapping rects', () => {
      expect(overlaps(rect, { left: 400, top: 500, right: 700, bottom: 800 })).toBe(true);
    });

    it('returns false for non-overlapping rects', () => {
      expect(overlaps(rect, { left: 600, top: 200, right: 800, bottom: 600 })).toBe(false);
    });

    it('returns false for adjacent rects (touching edges)', () => {
      expect(overlaps(rect, { left: 500, top: 200, right: 700, bottom: 600 })).toBe(false);
    });
  });

  describe('boundsEqual', () => {
    it('returns true for identical rects', () => {
      expect(boundsEqual(rect, { ...rect })).toBe(true);
    });

    it('returns false for different rects', () => {
      expect(boundsEqual(rect, { ...rect, right: 501 })).toBe(false);
    });
  });

  describe('parseBounds', () => {
    it('parses Android bounds string', () => {
      expect(parseBounds('[100,200][500,600]')).toEqual(rect);
    });

    it('parses zero-origin bounds', () => {
      expect(parseBounds('[0,0][1080,2400]')).toEqual({
        left: 0,
        top: 0,
        right: 1080,
        bottom: 2400,
      });
    });

    it('parses bounds with negative origin (partially off-screen element)', () => {
      expect(parseBounds('[-40,-20][1040,100]')).toEqual({
        left: -40,
        top: -20,
        right: 1040,
        bottom: 100,
      });
    });

    it('returns null for invalid format', () => {
      expect(parseBounds('invalid')).toBeNull();
      expect(parseBounds('[100,200]')).toBeNull();
    });
  });
});

describe('visible-region', () => {
  // Target element: a list row spanning most of the screen width
  const target: Rect = { left: 42, top: 1855, right: 1038, bottom: 2025 };

  describe('largestVisibleStrip', () => {
    it('returns null when fully obscured', () => {
      const obstructor: Rect = { left: 0, top: 1800, right: 1080, bottom: 2100 };
      expect(largestVisibleStrip(target, obstructor)).toBeNull();
    });

    it('finds bottom strip when obstructor covers top portion', () => {
      const obstructor: Rect = { left: 0, top: 1800, right: 1080, bottom: 1940 };
      const result = largestVisibleStrip(target, obstructor);
      expect(result).not.toBeNull();
      expect(result!.rect.top).toBe(1940);
      expect(result!.rect.bottom).toBe(2025);
    });

    it('finds left strip when FAB covers right side', () => {
      // FAB in bottom-right corner
      const fab: Rect = { left: 891, top: 1875, right: 1038, bottom: 2022 };
      const result = largestVisibleStrip(target, fab);
      expect(result).not.toBeNull();
      // Left strip should be largest (full height, left portion)
      expect(result!.rect.left).toBe(42);
      expect(result!.rect.right).toBe(891);
      expect(result!.area).toBeGreaterThan(0);
    });

    it('picks the largest strip among candidates', () => {
      // Small obstructor in the center — all 4 strips available
      const small: Rect = { left: 400, top: 1900, right: 600, bottom: 1980 };
      const result = largestVisibleStrip(target, small);
      expect(result).not.toBeNull();
      // Left strip: (400-42) * (2025-1855) = 358 * 170 = 60860
      // Right strip: (1038-600) * 170 = 438 * 170 = 74460
      // Right strip should win
      expect(result!.rect.left).toBe(600);
      expect(result!.rect.right).toBe(1038);
    });

    it('returns center point of the visible strip', () => {
      const fab: Rect = { left: 891, top: 1875, right: 1038, bottom: 2022 };
      const result = largestVisibleStrip(target, fab);
      expect(result).not.toBeNull();
      expect(result!.center.x).toBe(Math.floor((42 + 891) / 2));
      expect(result!.center.y).toBe(Math.floor((1855 + 2025) / 2));
    });
  });
});

describe('obstruction detection', () => {
  // Helper to create a minimal UiNode
  function makeNode(overrides: Partial<UiNode> = {}): UiNode {
    return {
      text: '', contentDesc: '', resourceId: '', className: '',
      packageName: '', bounds: null, clickable: false, focused: false,
      enabled: true, selected: false, drawingOrder: 0, children: [],
      ...overrides,
    };
  }

  const archiveRow = makeNode({
    text: 'Archive',
    bounds: { left: 42, top: 1855, right: 1038, bottom: 2025 },
    clickable: true,
  });

  const fab = makeNode({
    contentDesc: 'Add Item',
    bounds: { left: 891, top: 1875, right: 1038, bottom: 2022 },
    clickable: true,
  });

  // Hierarchy: root contains archiveRow and fab (fab is later = higher z-order)
  const hierarchy = makeNode({
    bounds: { left: 0, top: 0, right: 1080, bottom: 2400 },
    children: [archiveRow, fab],
  });

  const noOverlayWindows: WindowInfo[] = [];

  describe('clear path', () => {
    it('returns not obstructed when target is the topmost clickable', () => {
      // Tap center of archive row — only archiveRow contains this point, no FAB
      const result = detectObstruction({
        hierarchy,
        windows: noOverlayWindows,
        targetElement: archiveRow,
        tapPoint: { x: 200, y: 1940 },
        searchText: 'Archive',
      });
      expect(result.obstructed).toBe(false);
    });
  });

  describe('FAB obstruction', () => {
    it('detects FAB overlapping the target center', () => {
      // Tap at a point where both archive row and FAB overlap — FAB is later in tree
      const result = detectObstruction({
        hierarchy,
        windows: noOverlayWindows,
        targetElement: archiveRow,
        tapPoint: { x: 965, y: 1948 },
        searchText: 'Archive',
      });
      expect(result.obstructed).toBe(true);
      if (result.obstructed) {
        expect(result.obstructor).toContain('Add Item');
        expect(result.adjustedPoint).not.toBeNull();
        expect(result.fullyObscured).toBe(false);
        // Adjusted point should be in the left strip (away from FAB)
        expect(result.adjustedPoint!.x).toBeLessThan(891);
      }
    });
  });

  describe('system overlay', () => {
    it('detects TalkBack FloatingMenu overlay at tap point', () => {
      const talkbackWindows: WindowInfo[] = [
        {
          name: 'FloatingMenu',
          type: 'NAVIGATION_BAR_PANEL',
          hasSurface: true,
          touchableRegion: { left: 891, top: 1875, right: 1038, bottom: 2022 },
        },
      ];

      const result = detectObstruction({
        hierarchy,
        windows: talkbackWindows,
        targetElement: archiveRow,
        tapPoint: { x: 965, y: 1948 },
        searchText: 'Archive',
      });
      expect(result.obstructed).toBe(true);
      if (result.obstructed) {
        expect(result.obstructor).toContain('FloatingMenu');
        expect(result.adjustedPoint).not.toBeNull();
      }
    });

    it('system overlay takes precedence over in-app elements', () => {
      // Both a system overlay and FAB at the same point — system overlay detected first
      const talkbackWindows: WindowInfo[] = [
        {
          name: 'FloatingMenu',
          type: 'NAVIGATION_BAR_PANEL',
          hasSurface: true,
          touchableRegion: { left: 891, top: 1875, right: 1038, bottom: 2022 },
        },
      ];

      const result = detectObstruction({
        hierarchy,
        windows: talkbackWindows,
        targetElement: archiveRow,
        tapPoint: { x: 965, y: 1948 },
        searchText: 'Archive',
      });
      expect(result.obstructed).toBe(true);
      if (result.obstructed) {
        expect(result.obstructor).toContain('system_overlay');
      }
    });
  });

  describe('fully obscured', () => {
    it('reports fully obscured when obstructor covers entire target', () => {
      const fullScreenOverlay: WindowInfo[] = [
        {
          name: 'SystemDialog',
          type: 'SYSTEM_ALERT',
          hasSurface: true,
          touchableRegion: { left: 0, top: 0, right: 1080, bottom: 2400 },
        },
      ];

      const result = detectObstruction({
        hierarchy,
        windows: fullScreenOverlay,
        targetElement: archiveRow,
        tapPoint: { x: 540, y: 1940 },
        searchText: 'Archive',
      });
      expect(result.obstructed).toBe(true);
      if (result.obstructed) {
        expect(result.fullyObscured).toBe(true);
        expect(result.adjustedPoint).toBeNull();
      }
    });
  });

  describe('Compose parent wrapper', () => {
    it('treats identical bounds as parent wrapper, not obstruction', () => {
      // Compose pattern: clickable parent has same bounds as text child
      const textChild = makeNode({
        contentDesc: 'Download now',
        bounds: { left: 84, top: 553, right: 996, bottom: 679 },
        clickable: false,
      });
      const clickableParent = makeNode({
        bounds: { left: 84, top: 553, right: 996, bottom: 679 },
        clickable: true,
        children: [textChild],
      });
      const tree = makeNode({
        bounds: { left: 0, top: 0, right: 1080, bottom: 2400 },
        children: [clickableParent],
      });

      const result = detectObstruction({
        hierarchy: tree,
        windows: noOverlayWindows,
        targetElement: textChild,
        tapPoint: { x: 540, y: 616 },
        searchText: 'Download now',
      });
      expect(result.obstructed).toBe(false);
    });
  });
});
