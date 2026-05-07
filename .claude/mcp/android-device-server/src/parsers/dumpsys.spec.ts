import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseDumpsysWindows, findOverlayAtPoint } from './dumpsys.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const fixtureOutput = readFileSync(
  join(__dirname, '__fixtures__', 'dumpsys-windows.txt'),
  'utf-8',
);

describe('parseDumpsysWindows', () => {
  it('parses all windows from real dumpsys output', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    expect(windows.length).toBeGreaterThan(5);
  });

  it('extracts window names', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const names = windows.map(w => w.name);
    expect(names).toContain('FloatingMenu');
    expect(names).toContain('StatusBar');
  });

  it('extracts window types from mAttrs line', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const floatingMenu = windows.find(w => w.name === 'FloatingMenu');
    expect(floatingMenu).toBeDefined();
    expect(floatingMenu!.type).toBe('NAVIGATION_BAR_PANEL');
  });

  it('does not match ty= in ROTATION_ lines or mViewVisibility', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    // Taskbar has ROTATION_ lines with ty= — should only capture the mAttrs ty=
    const taskbar = windows.find(w => w.name === 'Taskbar');
    expect(taskbar).toBeDefined();
    expect(taskbar!.type).toBe('NAVIGATION_BAR');
  });

  it('extracts surface visibility', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const floatingMenu = windows.find(w => w.name === 'FloatingMenu');
    expect(floatingMenu!.hasSurface).toBe(true);

    const notificationShade = windows.find(w => w.name === 'NotificationShade');
    expect(notificationShade!.hasSurface).toBe(false);
  });

  it('extracts touchable region with coordinates', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const floatingMenu = windows.find(w => w.name === 'FloatingMenu');
    expect(floatingMenu!.touchableRegion).not.toBeNull();
    expect(floatingMenu!.touchableRegion!.left).toBeGreaterThanOrEqual(0);
    expect(floatingMenu!.touchableRegion!.right).toBeLessThanOrEqual(1080);
  });

  it('handles empty SkRegion() as null touchable region', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const screenDecorBottom = windows.find(w => w.name === 'ScreenDecorOverlayBottom');
    expect(screenDecorBottom).toBeDefined();
    expect(screenDecorBottom!.touchableRegion).toBeNull();
  });

  it('parses app window as BASE_APPLICATION type', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const appWindow = windows.find(w => w.name.includes('bitwarden'));
    expect(appWindow).toBeDefined();
    expect(appWindow!.type).toBe('BASE_APPLICATION');
  });
});

describe('findOverlayAtPoint', () => {
  it('finds FloatingMenu overlay at its touchable region', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    const floatingMenu = windows.find(w => w.name === 'FloatingMenu');
    expect(floatingMenu?.touchableRegion).not.toBeNull();

    const region = floatingMenu!.touchableRegion!;
    const center = {
      x: Math.floor((region.left + region.right) / 2),
      y: Math.floor((region.top + region.bottom) / 2),
    };

    const overlay = findOverlayAtPoint(windows, center);
    // Should find some overlay at this point (FloatingMenu or ScreenDecorOverlay)
    expect(overlay).not.toBeNull();
    expect(overlay!.type).not.toBe('BASE_APPLICATION');
  });

  it('returns null for point with no overlays', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    // Point in the middle of the screen — unlikely to have overlay touchable regions
    const overlay = findOverlayAtPoint(windows, { x: 540, y: 1000 });
    expect(overlay).toBeNull();
  });

  it('excludes BASE_APPLICATION windows', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    // The app window covers the whole screen but should never be returned
    const overlay = findOverlayAtPoint(windows, { x: 540, y: 1200 });
    if (overlay) {
      expect(overlay.type).not.toBe('BASE_APPLICATION');
    }
  });

  it('excludes windows without visible surface', () => {
    const windows = parseDumpsysWindows(fixtureOutput);
    // NotificationShade has touchable region but mHasSurface=false
    const shadeRegion = windows.find(w => w.name === 'NotificationShade')?.touchableRegion;
    if (shadeRegion) {
      const overlay = findOverlayAtPoint(windows, {
        x: Math.floor((shadeRegion.left + shadeRegion.right) / 2),
        y: Math.floor((shadeRegion.top + shadeRegion.bottom) / 2),
      });
      // Should not return NotificationShade since its surface is not visible
      if (overlay) {
        expect(overlay.name).not.toBe('NotificationShade');
      }
    }
  });
});
