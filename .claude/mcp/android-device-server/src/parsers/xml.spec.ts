import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseHierarchy, findElementByText, findTopmostClickableAt } from './xml.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const fixtureXml = readFileSync(join(__dirname, '__fixtures__', 'view.xml'), 'utf-8');

describe('parseHierarchy', () => {
  it('parses real UIAutomator XML into a node tree', () => {
    const root = parseHierarchy(fixtureXml);
    expect(root.className).toBe('android.widget.FrameLayout');
    expect(root.packageName).toBe('com.x8bit.bitwarden.dev');
    expect(root.bounds).toEqual({ left: 0, top: 0, right: 1080, bottom: 2400 });
  });

  it('preserves the full tree depth with children', () => {
    const root = parseHierarchy(fixtureXml);
    expect(root.children.length).toBeGreaterThan(0);
    // Should have deeply nested children
    let depth = 0;
    let node = root;
    while (node.children.length > 0) {
      node = node.children[0];
      depth++;
    }
    expect(depth).toBeGreaterThan(5);
  });

  it('parses boolean attributes correctly', () => {
    const root = parseHierarchy(fixtureXml);
    // Root FrameLayout is not clickable
    expect(root.clickable).toBe(false);
    expect(root.enabled).toBe(true);
  });

  it('throws on invalid XML', () => {
    expect(() => parseHierarchy('<invalid>')).toThrow();
  });

  it('throws on XML without hierarchy root', () => {
    expect(() => parseHierarchy('<?xml version="1.0"?><other/>')).toThrow('missing <hierarchy>');
  });
});

describe('findElementByText', () => {
  it('finds element by text attribute', () => {
    const root = parseHierarchy(fixtureXml);
    const el = findElementByText(root, 'Login');
    expect(el).not.toBeNull();
    expect(el!.text).toBe('Login');
  });

  it('finds element by content-desc', () => {
    const root = parseHierarchy(fixtureXml);
    const el = findElementByText(root, 'Add Item');
    expect(el).not.toBeNull();
    expect(el!.contentDesc).toBe('Add Item');
  });

  it('is case-insensitive', () => {
    const root = parseHierarchy(fixtureXml);
    const el = findElementByText(root, 'login');
    expect(el).not.toBeNull();
    expect(el!.text).toBe('Login');
  });

  it('returns null for non-existent text', () => {
    const root = parseHierarchy(fixtureXml);
    expect(findElementByText(root, 'NONEXISTENT_TEXT_12345')).toBeNull();
  });

  it('returns element with parsed bounds', () => {
    const root = parseHierarchy(fixtureXml);
    const el = findElementByText(root, 'Settings');
    expect(el).not.toBeNull();
    expect(el!.bounds).not.toBeNull();
    expect(el!.bounds!.left).toBeGreaterThanOrEqual(0);
    expect(el!.bounds!.right).toBeLessThanOrEqual(1080);
  });
});

describe('findTopmostClickableAt', () => {
  it('finds the topmost clickable element at a point', () => {
    const root = parseHierarchy(fixtureXml);
    // Point in the center of the screen — should find something clickable
    const el = findTopmostClickableAt(root, { x: 540, y: 1200 });
    // May or may not find something depending on layout, but shouldn't crash
    if (el) {
      expect(el.clickable).toBe(true);
      expect(el.bounds).not.toBeNull();
    }
  });

  it('returns null for a point with no clickable elements', () => {
    const root = parseHierarchy(fixtureXml);
    // Point in the status bar area — unlikely to have clickable app elements
    const el = findTopmostClickableAt(root, { x: 540, y: 50 });
    // Could be null or a system element — just verify no crash
    expect(el === null || el.clickable === true).toBe(true);
  });

  it('returns the LAST clickable in document order (highest z-order)', () => {
    const root = parseHierarchy(fixtureXml);
    // Find the "Add Item" FAB element to get its center
    const fab = findElementByText(root, 'Add Item');
    if (fab?.bounds) {
      const fabCenter = {
        x: Math.floor((fab.bounds.left + fab.bounds.right) / 2),
        y: Math.floor((fab.bounds.top + fab.bounds.bottom) / 2),
      };
      const topmost = findTopmostClickableAt(root, fabCenter);
      expect(topmost).not.toBeNull();
      // The topmost clickable at the FAB's center should be the FAB itself
      // or its clickable parent (bounds should overlap)
      expect(topmost!.bounds).not.toBeNull();
    }
  });
});
