/**
 * Tap at coordinates tool — tap a specific screen location, wait, and capture screenshot.
 */

import { z } from 'zod';
import { resolve } from 'node:path';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import * as adb from '../adb/adb.js';

const TapAtSchema = z.object({
  x: z.number().int().nonnegative(),
  y: z.number().int().nonnegative(),
  waitSeconds: z.number().min(0).default(2),
});

const tapAt: ToolDefinition = {
  name: 'tap_at',
  description:
    'Tap at specific screen coordinates, wait for the UI to settle, and capture a screenshot. ' +
    'Returns the path to the captured screenshot.',
  inputSchema: {
    type: 'object',
    properties: {
      x: { type: 'number', description: 'X coordinate to tap' },
      y: { type: 'number', description: 'Y coordinate to tap' },
      waitSeconds: { type: 'number', description: 'Seconds to wait after tap before capture (default: 2)' },
    },
    required: ['x', 'y'],
  },
  async handler(input: unknown): Promise<string> {
    const { x, y, waitSeconds } = validateInput(TapAtSchema, input);

    await adb.tap(x, y);
    await adb.sleep(waitSeconds ?? 2);

    const pngPath = resolve('screen.png');
    await adb.screenshot(pngPath);

    return `Tapped at (${x}, ${y}), waited ${waitSeconds}s\nScreenshot saved to: ${pngPath}`;
  },
};

export default tapAt;
