/**
 * Tap element tool — find an element by text, tap it, and capture screenshot.
 * Uses the shared find-element pipeline for obstruction detection.
 */

import { z } from 'zod';
import { resolve } from 'node:path';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import * as adb from '../adb/adb.js';
import { findElementWithObstruction } from './find-element-pipeline.js';

const TapElementSchema = z.object({
  text: z.string().min(1),
  waitSeconds: z.number().min(0).default(2),
});

const tapElement: ToolDefinition = {
  name: 'tap_element',
  description:
    'Find a UI element by text or content-desc, tap it, and capture a screenshot. ' +
    'Automatically detects obstructions and adjusts tap coordinates to the largest visible region. ' +
    'Returns element info, tap coordinates, obstruction status, and screenshot path.',
  inputSchema: {
    type: 'object',
    properties: {
      text: { type: 'string', description: 'Text or content-desc of the element to tap' },
      waitSeconds: { type: 'number', description: 'Seconds to wait after tap before capture (default: 2)' },
    },
    required: ['text'],
  },
  async handler(input: unknown): Promise<string> {
    const { text, waitSeconds } = validateInput(TapElementSchema, input);

    const outcome = await findElementWithObstruction(text);
    if ('error' in outcome) return `Error: ${outcome.error}`;

    const { target, effectivePoint, obstruction } = outcome.result;
    const lines: string[] = [];

    lines.push(`Element found: "${target.text || target.contentDesc}"`);

    if (obstruction.obstructed) {
      lines.push(`WARNING: Obstructed by ${obstruction.obstructor}`);
      if (obstruction.fullyObscured) {
        lines.push('FULLY OBSCURED — tapping original center as best effort');
      } else {
        lines.push(`Using adjusted coordinates: (${effectivePoint.x}, ${effectivePoint.y})`);
      }
    }

    await adb.tap(effectivePoint.x, effectivePoint.y);
    await adb.sleep(waitSeconds ?? 2);

    const pngPath = resolve('screen.png');
    await adb.screenshot(pngPath);

    lines.push(`Tapped at (${effectivePoint.x}, ${effectivePoint.y})`);
    lines.push(`Screenshot saved to: ${pngPath}`);

    return lines.join('\n');
  },
};

export default tapElement;
