#!/usr/bin/env node

/**
 * Android Device MCP Server
 * MCP server for Android device interaction via ADB — UI hierarchy capture,
 * element finding with obstruction detection, tap, and navigation.
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import type { ToolDefinition } from './utils/validation.js';
import capture from './tools/capture.js';
import findElement from './tools/find-element.js';
import tapAt from './tools/tap-at.js';
import tapElement from './tools/tap-element.js';
import navigate from './tools/navigate.js';
import inputText from './tools/input-text.js';

const tools: ToolDefinition[] = [capture, findElement, tapAt, tapElement, navigate, inputText];

async function main() {
  const server = new Server(
    { name: 'android-device-mcp', version: '1.0.0' },
    { capabilities: { tools: {} } },
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: tools.map(t => ({
      name: t.name,
      description: t.description,
      inputSchema: t.inputSchema,
    })),
  }));

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    const tool = tools.find(t => t.name === name);

    if (!tool) {
      throw new Error(`Unknown tool: ${name}`);
    }

    try {
      const result = await tool.handler(args || {});
      return { content: [{ type: 'text', text: result }] };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      console.error(`Tool error (${name}):`, message);
      return { content: [{ type: 'text', text: `Error: ${message}` }], isError: true };
    }
  });

  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
