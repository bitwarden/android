/**
 * Find element tool — locate a UI element by text/content-desc with obstruction detection.
 */

import { z } from 'zod';
import type { ToolDefinition } from '../utils/validation.js';
import { validateInput } from '../utils/validation.js';
import { findElementWithObstruction } from './find-element-pipeline.js';

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

    const outcome = await findElementWithObstruction(text);
    if ('error' in outcome) return outcome.error;

    const { target, tapPoint, effectivePoint, obstruction } = outcome.result;
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
