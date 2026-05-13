/**
 * Input validation and tool definition types.
 */

import { z } from 'zod';

/**
 * Shape of a tool module's default export.
 * Each tool file exports a ToolDefinition with metadata and a handler function.
 */
export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: any;
  handler: (input: any) => Promise<any>;
}

/**
 * Validate input against a Zod schema.
 * @throws {Error} with formatted validation messages on failure
 */
export function validateInput<T>(schema: z.ZodSchema<T>, input: unknown): T {
  try {
    return schema.parse(input);
  } catch (error) {
    if (error instanceof z.ZodError) {
      const messages = error.errors.map(e => `${e.path.join('.')}: ${e.message}`);
      throw new Error(`Validation failed: ${messages.join(', ')}`);
    }
    throw error;
  }
}
