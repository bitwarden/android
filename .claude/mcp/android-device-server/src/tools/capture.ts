/**
 * Capture tool — dump UI hierarchy XML and/or screenshot from the connected device.
 */

import { z } from 'zod';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import * as adb from '../adb/adb.js';
import { resolve } from 'node:path';

const CaptureSchema = z.object({
  xml: z.boolean().optional().default(true),
  screenshot: z.boolean().optional().default(true),
});

const capture: ToolDefinition = {
  name: 'capture',
  description:
    'Capture current Android device state. Dumps UI hierarchy XML and/or takes a screenshot. ' +
    'Files are saved to the current working directory as view.xml and screen.png.',
  inputSchema: {
    type: 'object',
    properties: {
      xml: { type: 'boolean', description: 'Capture UI hierarchy XML (default: true)' },
      screenshot: { type: 'boolean', description: 'Capture screenshot (default: true)' },
    },
  },
  async handler(input: unknown): Promise<string> {
    const { xml, screenshot } = validateInput(CaptureSchema, input);
    const results: string[] = [];

    if (xml) {
      const xmlPath = resolve('view.xml');
      await adb.dumpHierarchy(xmlPath);
      results.push(`UI hierarchy saved to: ${xmlPath}`);
    }

    if (screenshot) {
      const pngPath = resolve('screen.png');
      await adb.screenshot(pngPath);
      results.push(`Screenshot saved to: ${pngPath}`);
    }

    if (results.length === 0) {
      return 'Nothing captured. Set xml and/or screenshot to true.';
    }

    return results.join('\n');
  },
};

export default capture;
