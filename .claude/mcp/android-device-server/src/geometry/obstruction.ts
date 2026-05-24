/**
 * Two-layer obstruction detection for UI elements.
 *
 * Layer 1: System overlay windows (TalkBack, PiP, accessibility services)
 *          detected via parsed `dumpsys window windows` output.
 * Layer 2: In-app elements (FABs, dialogs, bottom sheets) detected via
 *          the UIAutomator XML hierarchy — topmost clickable at tap point.
 *
 * When obstruction is found, computes an alternative tap point using the
 * largest visible strip of the target element not covered by the obstructor.
 */

import { type Point, type Rect, center, boundsEqual } from './bounds.js';
import { largestVisibleStrip, type VisibleStrip } from './visible-region.js';
import { type UiNode, findTopmostClickableAt } from '../parsers/xml.js';
import { type WindowInfo, findOverlayAtPoint } from '../parsers/dumpsys.js';

export type ObstructionResult =
  | { obstructed: false }
  | {
      obstructed: true;
      obstructor: string;
      obstructorBounds: Rect;
      adjustedPoint: Point | null;
      visibleRegion: VisibleStrip | null;
      fullyObscured: boolean;
    };

export interface DetectObstructionParams {
  hierarchy: UiNode;
  windows: WindowInfo[];
  targetElement: UiNode;
  tapPoint: Point;
  searchText: string;
}

/**
 * Detect if the tap point is obstructed by a system overlay or in-app element.
 */
export function detectObstruction(params: DetectObstructionParams): ObstructionResult {
  const { hierarchy, windows, targetElement, tapPoint, searchText } = params;

  // Layer 1: System overlays (TalkBack, PiP, accessibility services)
  const overlay = findOverlayAtPoint(windows, tapPoint);
  if (overlay) {
    return buildResult(
      `system_overlay window=${overlay.name} type=${overlay.type}`,
      overlay.touchableRegion!,
      targetElement,
    );
  }

  // Layer 2: In-app elements (FABs, dialogs, bottom sheets)
  const topmost = findTopmostClickableAt(hierarchy, tapPoint);
  if (topmost && topmost.bounds) {
    // Check if topmost IS the target (no obstruction)
    if (isTargetMatch(topmost, targetElement, searchText)) {
      return { obstructed: false };
    }

    return buildResult(
      formatElementId(topmost),
      topmost.bounds,
      targetElement,
    );
  }

  return { obstructed: false };
}

/**
 * Check if the topmost clickable element matches the target.
 *
 * Match criteria:
 * - Search text appears in topmost's text or contentDesc
 * - Bounds are identical (Compose parent wrapper pattern)
 */
function isTargetMatch(topmost: UiNode, target: UiNode, searchText: string): boolean {
  const lower = searchText.toLowerCase();

  // Text/content-desc match
  if (topmost.text.toLowerCase().includes(lower)) return true;
  if (topmost.contentDesc.toLowerCase().includes(lower)) return true;

  // Bounds equality (Compose parent wrapper)
  if (target.bounds && topmost.bounds && boundsEqual(target.bounds, topmost.bounds)) {
    return true;
  }

  return false;
}

function buildResult(
  obstructorId: string,
  obstructorBounds: Rect,
  target: UiNode,
): ObstructionResult {
  if (!target.bounds) {
    return {
      obstructed: true,
      obstructor: obstructorId,
      obstructorBounds,
      adjustedPoint: null,
      visibleRegion: null,
      fullyObscured: true,
    };
  }

  const strip = largestVisibleStrip(target.bounds, obstructorBounds);

  return {
    obstructed: true,
    obstructor: obstructorId,
    obstructorBounds,
    adjustedPoint: strip?.center ?? null,
    visibleRegion: strip ?? null,
    fullyObscured: strip === null,
  };
}

function formatElementId(node: UiNode): string {
  if (node.text) return `text="${node.text}"`;
  if (node.contentDesc) return `desc="${node.contentDesc}"`;
  if (node.resourceId) return `id="${node.resourceId}"`;
  if (node.bounds) return `bounds=[${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}]`;
  return 'unknown';
}
