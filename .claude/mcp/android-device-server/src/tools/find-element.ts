/**
 * Find element tool — locate a UI element by text/content-desc with obstruction detection.
 *
 * This is the most complex tool. It:
 * 1. Dumps the UI hierarchy and parses it into a typed tree
 * 2. Searches for the target element by text or content-desc
 * 3. Runs two-layer obstruction detection (system overlays + in-app elements)
 * 4. Returns coordinates, element info, and obstruction status
 */

import { z } from 'zod';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import * as adb from '../adb/adb.js';
import { center } from '../geometry/bounds.js';
import { detectObstruction } from '../geometry/obstruction.js';
import { parseHierarchy, findElementByText } from '../parsers/xml.js';
import { parseDumpsysWindows } from '../parsers/dumpsys.js';

const FindElementSchema = z.object({
  text: z.string().min(1),
});

const findElement: ToolDefinition = {
  name: 'find_element',
  description:
    'Find a UI element by text or content-desc and return tap coordinates. ' +
    'Includes two-layer obstruction detection: system overlays (TalkBack, PiP) via dumpsys, ' +
    'and in-app elements (FABs, dialogs) via the UI hierarchy. When obstructed, returns ' +
    'adjusted coordinates targeting the largest visible region of the element.',
  inputSchema: {
    type: 'object',
    properties: {
      text: { type: 'string', description: 'Text or content-desc to search for' },
    },
    required: ['text'],
  },
  async handler(input: unknown): Promise<string> {
    const { text } = validateInput(FindElementSchema, input);

    // Dump hierarchy and parse
    const xmlPath = resolve('view.xml');
    await adb.dumpHierarchy(xmlPath);
    const xml = readFileSync(xmlPath, 'utf-8');
    const hierarchy = parseHierarchy(xml);

    // Find the target element
    const target = findElementByText(hierarchy, text);
    if (!target) {
      return `Element not found: "${text}"\n\nNo element with matching text or content-desc was found in the UI hierarchy.`;
    }

    if (!target.bounds) {
      return `Element found but has no bounds: "${text}"`;
    }

    const tapPoint = center(target.bounds);

    // Run obstruction detection
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

    // Build response
    const effectivePoint = obstruction.obstructed && obstruction.adjustedPoint
      ? obstruction.adjustedPoint
      : tapPoint;

    const lines: string[] = [];

    if (!obstruction.obstructed) {
      lines.push(`Element found: "${target.text || target.contentDesc}"`);
      lines.push(`Coordinates: (${effectivePoint.x}, ${effectivePoint.y})`);
      lines.push('Status: CLEAR');
    } else {
      lines.push(`Element found: "${target.text || target.contentDesc}"`);
      lines.push(`Status: OBSTRUCTED by ${obstruction.obstructor}`);
      if (obstruction.fullyObscured) {
        lines.push(`Coordinates: (${effectivePoint.x}, ${effectivePoint.y}) — FULLY OBSCURED, original center used`);
      } else {
        lines.push(`Adjusted coordinates: (${effectivePoint.x}, ${effectivePoint.y}) — center of largest visible strip`);
      }
    }

    const result = {
      found: true,
      text: target.text,
      contentDesc: target.contentDesc,
      resourceId: target.resourceId,
      bounds: target.bounds,
      center: tapPoint,
      effectivePoint,
      obstructed: obstruction.obstructed,
      ...(obstruction.obstructed ? {
        obstructor: obstruction.obstructor,
        obstructorBounds: obstruction.obstructorBounds,
        fullyObscured: obstruction.fullyObscured,
        visibleRegion: obstruction.visibleRegion?.rect ?? null,
      } : {}),
    };

    lines.push('');
    lines.push('```json');
    lines.push(JSON.stringify(result, null, 2));
    lines.push('```');

    return lines.join('\n');
  },
};

export default findElement;
