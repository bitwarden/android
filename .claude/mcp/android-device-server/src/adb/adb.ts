/**
 * ADB client wrapper using child_process.execFile for safe command execution.
 * Uses execFile (not exec) to prevent shell injection — arguments are passed
 * as an array, never interpolated into a shell string.
 */

import { execFile as execFileCb, execFileSync } from 'node:child_process';
import { promisify } from 'node:util';
import { existsSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

const execFile = promisify(execFileCb);

let cachedAdbPath: string | null = null;

/** Clear the cached ADB path (for testing). */
export function _resetCache(): void {
  cachedAdbPath = null;
}

/**
 * Discover ADB binary location.
 * Checks: PATH → ~/Library/Android/sdk/platform-tools/adb → /usr/local/bin/adb
 */
export function findAdb(): string {
  if (cachedAdbPath) return cachedAdbPath;

  // Check PATH via `which`
  try {
    const result = execFileSync('which', ['adb'], { encoding: 'utf-8' }).trim();
    if (result) {
      cachedAdbPath = result;
      return result;
    }
  } catch {
    // Not in PATH, try common locations
  }

  const candidates = [
    join(homedir(), 'Library', 'Android', 'sdk', 'platform-tools', 'adb'),
    '/usr/local/bin/adb',
  ];

  for (const candidate of candidates) {
    if (existsSync(candidate)) {
      cachedAdbPath = candidate;
      return candidate;
    }
  }

  throw new Error(
    'ADB not found. Install the Android SDK or add platform-tools to PATH.',
  );
}

/**
 * Execute an ADB command and return stdout.
 */
export async function exec(args: string[]): Promise<string> {
  const adb = findAdb();
  const { stdout } = await execFile(adb, args, {
    maxBuffer: 10 * 1024 * 1024, // 10MB for large dumps
    encoding: 'utf-8',
  });
  return stdout;
}

/**
 * Execute an ADB shell command.
 */
export async function shell(command: string): Promise<string> {
  return exec(['shell', command]);
}

/**
 * Dump UI hierarchy to device, then pull to local path.
 */
export async function dumpHierarchy(outputPath: string): Promise<void> {
  await shell('uiautomator dump /sdcard/view.xml');
  await exec(['pull', '/sdcard/view.xml', outputPath]);
}

/**
 * Capture screenshot to device, then pull to local path.
 */
export async function screenshot(outputPath: string): Promise<void> {
  await shell('screencap -p /sdcard/screen.png');
  await exec(['pull', '/sdcard/screen.png', outputPath]);
}

/**
 * Tap at screen coordinates.
 */
export async function tap(x: number, y: number): Promise<void> {
  await shell(`input tap ${Math.floor(x)} ${Math.floor(y)}`);
}

/**
 * Send a key event.
 */
export async function keyevent(code: number): Promise<void> {
  await shell(`input keyevent ${code}`);
}

/**
 * Perform a swipe gesture.
 */
export async function swipe(
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  durationMs: number,
): Promise<void> {
  await shell(`input swipe ${x1} ${y1} ${x2} ${y2} ${durationMs}`);
}

/**
 * Get screen dimensions.
 */
export async function getScreenSize(): Promise<{ width: number; height: number }> {
  const output = await shell('wm size');
  const match = output.match(/(\d+)x(\d+)/);
  if (!match) throw new Error(`Could not parse screen size from: ${output}`);
  return { width: parseInt(match[1], 10), height: parseInt(match[2], 10) };
}

/**
 * Get raw dumpsys window windows output.
 */
export async function dumpsysWindows(): Promise<string> {
  return shell('dumpsys window windows');
}

/**
 * Wait for a specified duration (seconds).
 */
export function sleep(seconds: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, seconds * 1000));
}
