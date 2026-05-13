/**
 * Input text tool — type text into the focused field, with optional clearing.
 */

import { z } from 'zod';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import * as adb from '../adb/adb.js';

const KEYCODE_MOVE_END = 123;
const KEYCODE_DEL = 67;

const InputTextSchema = z.object({
  text: z.string().min(1),
  clear: z.boolean().default(false),
});

/**
 * Clear the currently focused text field by moving to the end and
 * sending enough delete key events to remove all characters.
 * Uses a generous count to ensure complete clearing.
 */
async function clearField(): Promise<void> {
  await adb.keyevent(KEYCODE_MOVE_END);
  // Send 50 deletes — more than enough for any reasonable field length.
  // ADB processes them almost instantly and extras on an empty field are no-ops.
  const deletes = Array(50).fill(String(KEYCODE_DEL)).join(' ');
  await adb.shell(`input keyevent ${deletes}`);
}

const inputText: ToolDefinition = {
  name: 'input_text',
  description:
    'Type text into the currently focused input field. Optionally clear existing content first. ' +
    'The field must already be focused (tap it first if needed).',
  inputSchema: {
    type: 'object',
    properties: {
      text: { type: 'string', description: 'Text to type into the focused field' },
      clear: {
        type: 'boolean',
        description: 'Clear existing field content before typing (default: false)',
      },
    },
    required: ['text'],
  },
  async handler(input: unknown): Promise<string> {
    const { text, clear } = validateInput(InputTextSchema, input);

    if (clear) {
      await clearField();
    }

    // Escape characters that the Android shell interprets inside double quotes:
    // " $ ` \ are all special in sh double-quoted strings.
    const escaped = text
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"')
      .replace(/\$/g, '\\$')
      .replace(/`/g, '\\`');
    await adb.shell(`input text "${escaped}"`);

    const lines: string[] = [];
    if (clear) lines.push('Cleared existing content');
    lines.push(`Typed: "${text}"`);
    return lines.join('\n');
  },
};

export default inputText;