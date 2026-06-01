/**
 * Shared pipeline for finding a UI element with obstruction detection.
 * Used by both find_element and tap_element tools.
 */

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import * as adb from '../adb/adb.js';
import { type Point, center } from '../geometry/bounds.js';
import { detectObstruction, type ObstructionResult } from '../geometry/obstruction.js';
import { parseHierarchy, findElementByText, type UiNode } from '../parsers/xml.js';
import { parseDumpsysWindows } from '../parsers/dumpsys.js';

export interface FindElementResult {
  target: UiNode;
  tapPoint: Point;
  effectivePoint: Point;
  obstruction: ObstructionResult;
}

/**
 * Dump hierarchy, find element by text, run obstruction detection.
 * Returns null with an error message if element not found.
 */
export async function findElementWithObstruction(
  text: string,
): Promise<{ result: FindElementResult } | { error: string }> {
  const xmlPath = resolve('view.xml');
  await adb.dumpHierarchy(xmlPath);
  const xml = readFileSync(xmlPath, 'utf-8');
  const hierarchy = parseHierarchy(xml);

  const target = findElementByText(hierarchy, text);
  if (!target) {
    return { error: `Element not found: "${text}"\n\nNo element with matching text or content-desc was found in the UI hierarchy.` };
  }

  if (!target.bounds) {
    return { error: `Element found but has no bounds: "${text}"` };
  }

  const tapPoint = center(target.bounds);

  let dumpsysOutput: string;
  try {
    dumpsysOutput = await adb.dumpsysWindows();
  } catch {
    dumpsysOutput = '';
  }
  const windows = parseDumpsysWindows(dumpsysOutput);

  const obstruction = detectObstruction({
    hierarchy,
    windows,
    targetElement: target,
    tapPoint,
    searchText: text,
  });

  const effectivePoint = obstruction.obstructed && obstruction.adjustedPoint
    ? obstruction.adjustedPoint
    : tapPoint;

  return { result: { target, tapPoint, effectivePoint, obstruction } };
}
