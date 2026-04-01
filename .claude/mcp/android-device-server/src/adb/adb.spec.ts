import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('node:fs', () => ({
  existsSync: vi.fn(() => false),
}));

vi.mock('node:child_process', () => ({
  execFileSync: vi.fn(() => { throw new Error('not found'); }),
  execFile: vi.fn(),
}));

import { existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { findAdb, _resetCache } from './adb.js';

const mockExistsSync = vi.mocked(existsSync);
const mockExecFileSync = vi.mocked(execFileSync);

describe('findAdb', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    _resetCache();
    // Default: which fails, nothing on disk
    mockExecFileSync.mockImplementation(() => { throw new Error('not found'); });
    mockExistsSync.mockReturnValue(false);
  });

  it('finds adb in PATH via which', () => {
    mockExecFileSync.mockReturnValue('/usr/local/bin/adb\n' as any);
    expect(findAdb()).toBe('/usr/local/bin/adb');
  });

  it('finds adb in Android SDK location', () => {
    mockExistsSync.mockImplementation((path) =>
      String(path).includes('Library/Android/sdk'),
    );
    expect(findAdb()).toContain('Library/Android/sdk/platform-tools/adb');
  });

  it('finds adb in /usr/local/bin', () => {
    mockExistsSync.mockImplementation((path) =>
      String(path) === '/usr/local/bin/adb',
    );
    expect(findAdb()).toBe('/usr/local/bin/adb');
  });

  it('throws when adb not found anywhere', () => {
    expect(() => findAdb()).toThrow('ADB not found');
  });

  it('caches the result after first discovery', () => {
    mockExistsSync.mockImplementation((path) =>
      String(path) === '/usr/local/bin/adb',
    );
    findAdb();
    findAdb();
    // existsSync only called during first discovery, cached after
    expect(mockExistsSync).toHaveBeenCalledTimes(2); // SDK path + /usr/local/bin
  });
});
