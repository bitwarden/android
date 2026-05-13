/**
 * Navigate tool — perform common navigation actions on the device.
 */

import { z } from 'zod';
import { resolve } from 'node:path';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import * as adb from '../adb/adb.js';

const NavigateSchema = z.object({
  action: z.enum(['home', 'back', 'app-drawer']),
  waitSeconds: z.number().min(0).default(1),
});

const navigate: ToolDefinition = {
  name: 'navigate',
  description:
    'Perform a navigation action on the Android device: go home, press back, or open the app drawer. ' +
    'Captures a screenshot after the action completes.',
  inputSchema: {
    type: 'object',
    properties: {
      action: {
        type: 'string',
        enum: ['home', 'back', 'app-drawer'],
        description: 'Navigation action to perform',
      },
      waitSeconds: { type: 'number', description: 'Seconds to wait after action before capture (default: 1)' },
    },
    required: ['action'],
  },
  async handler(input: unknown): Promise<string> {
    const { action, waitSeconds } = validateInput(NavigateSchema, input);

    switch (action) {
      case 'home':
        await adb.keyevent(3);
        break;
      case 'back':
        await adb.keyevent(4);
        break;
      case 'app-drawer': {
        const screen = await adb.getScreenSize();
        const cx = Math.floor(screen.width / 2);
        const fromY = Math.floor(screen.height * 0.93);
        const toY = Math.floor(screen.height * 0.17);
        await adb.swipe(cx, fromY, cx, toY, 1000);
        break;
      }
    }

    await adb.sleep(waitSeconds ?? 1);

    const pngPath = resolve('screen.png');
    await adb.screenshot(pngPath);

    return `Navigated: ${action}\nScreenshot saved to: ${pngPath}`;
  },
};

export default navigate;
