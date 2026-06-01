/**
 * UIAutomator XML hierarchy parser.
 *
 * Converts Android's single-line UIAutomator XML dump into a typed, traversable
 * node tree. Replaces the fragile grep/awk approach from the shell scripts.
 */

import { XMLParser } from 'fast-xml-parser';
import { type Rect, type Point, parseBounds, containsPoint } from '../geometry/bounds.js';

export interface UiNode {
  text: string;
  contentDesc: string;
  resourceId: string;
  className: string;
  packageName: string;
  bounds: Rect | null;
  clickable: boolean;
  focused: boolean;
  enabled: boolean;
  selected: boolean;
  drawingOrder: number;
  children: UiNode[];
}

const parser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '',
  // Ensure 'node' is always an array even when there's only one child
  isArray: (name) => name === 'node',
});

/**
 * Parse a UIAutomator XML dump into a typed node tree.
 */
export function parseHierarchy(xml: string): UiNode {
  const parsed = parser.parse(xml);
  const hierarchy = parsed?.hierarchy;
  if (!hierarchy) {
    throw new Error('Invalid UIAutomator XML: missing <hierarchy> root');
  }

  const rootNodes = hierarchy.node;
  if (!rootNodes || !Array.isArray(rootNodes) || rootNodes.length === 0) {
    throw new Error('Invalid UIAutomator XML: no nodes found');
  }

  return convertNode(rootNodes[0]);
}

function convertNode(raw: any): UiNode {
  const children: UiNode[] = [];
  if (raw.node) {
    const childNodes = Array.isArray(raw.node) ? raw.node : [raw.node];
    for (const child of childNodes) {
      children.push(convertNode(child));
    }
  }

  return {
    text: raw.text ?? '',
    contentDesc: raw['content-desc'] ?? '',
    resourceId: raw['resource-id'] ?? '',
    className: raw.class ?? '',
    packageName: raw.package ?? '',
    bounds: parseBounds(raw.bounds ?? ''),
    clickable: raw.clickable === 'true',
    focused: raw.focused === 'true',
    enabled: raw.enabled === 'true',
    selected: raw.selected === 'true',
    drawingOrder: parseInt(raw['drawing-order'] ?? '0', 10),
    children,
  };
}

/**
 * Find the first element matching search text in text or content-desc.
 * Searches depth-first.
 */
export function findElementByText(root: UiNode, searchText: string): UiNode | null {
  const lower = searchText.toLowerCase();

  function search(node: UiNode): UiNode | null {
    if (
      node.text.toLowerCase().includes(lower) ||
      node.contentDesc.toLowerCase().includes(lower)
    ) {
      return node;
    }
    for (const child of node.children) {
      const found = search(child);
      if (found) return found;
    }
    return null;
  }

  return search(root);
}

/**
 * Find the topmost clickable element at a given point.
 *
 * In UIAutomator's depth-first XML, the LAST clickable element whose bounds
 * contain the point is the one that receives the tap (highest z-order at that
 * point). This traverses the full tree and returns the last match.
 */
export function findTopmostClickableAt(root: UiNode, point: Point): UiNode | null {
  let result: UiNode | null = null;

  function traverse(node: UiNode): void {
    if (node.clickable && node.bounds && containsPoint(node.bounds, point)) {
      result = node;
    }
    for (const child of node.children) {
      traverse(child);
    }
  }

  traverse(root);
  return result;
}
