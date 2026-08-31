#!/usr/bin/env node
/**
 * WCAG color contrast checker.
 *
 * Reads a JSON palette file describing foreground/background pairs and
 * validates each pair against the requested WCAG conformance level.
 *
 * Usage:
 *   node check-color-contrast.js <palette.json>
 *
 * Palette JSON shape (see ../assets/color-contrast-template.json):
 * {
 *   "checks": [
 *     {
 *       "name": "Primary button text",
 *       "foreground": "#ffffff",
 *       "background": "#0057b8",
 *       "fontSize": 16,
 *       "bold": false,
 *       "isUiComponent": false,
 *       "level": "AA"
 *     }
 *   ]
 * }
 *
 * - fontSize is in CSS pixels. bold defaults to false.
 * - "Large text" per WCAG is >= 18.66px, or >= 14px (>=~18.66px unbolded,
 *   >=14pt/~18.66px bold uses 14px threshold per WCAG's 14pt bold rule).
 *   This script treats >=24px normal or >=18.66px bold as "large" using the
 *   standard 18pt/14pt-bold definition (24px ≈ 18pt, 18.66px ≈ 14pt).
 * - isUiComponent: true applies the 3:1 non-text contrast rule (1.4.11)
 *   instead of the text contrast rule (1.4.3/1.4.6), regardless of fontSize.
 * - level: "AA" (default) or "AAA".
 *
 * Exits with a non-zero status code if any check fails.
 */

const fs = require('fs');
const path = require('path');

function fail(message) {
  console.error(`Error: ${message}`);
  process.exit(2);
}

const inputPath = process.argv[2];
if (!inputPath) {
  fail('missing palette JSON path.\nUsage: node check-color-contrast.js <palette.json>');
}

const resolvedPath = path.resolve(process.cwd(), inputPath);
if (!fs.existsSync(resolvedPath)) {
  fail(`file not found: ${resolvedPath}`);
}

let data;
try {
  data = JSON.parse(fs.readFileSync(resolvedPath, 'utf8'));
} catch (err) {
  fail(`invalid JSON in ${resolvedPath}: ${err.message}`);
}

const checks = Array.isArray(data.checks) ? data.checks : [];
if (checks.length === 0) {
  fail('palette JSON must contain a non-empty "checks" array.');
}

function parseHexColor(hex) {
  const normalized = hex.trim().replace(/^#/, '');
  const full =
    normalized.length === 3
      ? normalized
          .split('')
          .map((c) => c + c)
          .join('')
      : normalized;

  if (!/^[0-9a-fA-F]{6}$/.test(full)) {
    throw new Error(`invalid hex color: "${hex}"`);
  }

  return {
    r: parseInt(full.slice(0, 2), 16),
    g: parseInt(full.slice(2, 4), 16),
    b: parseInt(full.slice(4, 6), 16),
  };
}

function relativeLuminance({ r, g, b }) {
  const channel = (value) => {
    const c = value / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

function contrastRatio(hexA, hexB) {
  const lumA = relativeLuminance(parseHexColor(hexA));
  const lumB = relativeLuminance(parseHexColor(hexB));
  const lighter = Math.max(lumA, lumB);
  const darker = Math.min(lumA, lumB);
  return (lighter + 0.05) / (darker + 0.05);
}

function isLargeText(fontSize, bold) {
  if (!fontSize) return false;
  return bold ? fontSize >= 18.66 : fontSize >= 24;
}

function requiredRatio(check) {
  const level = (check.level || 'AA').toUpperCase();

  if (check.isUiComponent) {
    // WCAG 1.4.11 Non-text Contrast: 3:1 at both AA and AAA (no AAA delta defined).
    return 3;
  }

  const large = isLargeText(check.fontSize, check.bold);
  if (level === 'AAA') {
    return large ? 4.5 : 7;
  }
  // Default: AA
  return large ? 3 : 4.5;
}

let hasFailure = false;
const results = [];

for (const check of checks) {
  const { name, foreground, background } = check;
  if (!foreground || !background) {
    results.push({ name: name || '(unnamed check)', error: 'missing foreground/background' });
    hasFailure = true;
    continue;
  }

  try {
    const ratio = contrastRatio(foreground, background);
    const required = requiredRatio(check);
    const pass = ratio >= required;
    if (!pass) hasFailure = true;

    results.push({
      name: name || `${foreground} on ${background}`,
      foreground,
      background,
      ratio: Math.round(ratio * 100) / 100,
      required,
      pass,
    });
  } catch (err) {
    results.push({ name: name || '(unnamed check)', error: err.message });
    hasFailure = true;
  }
}

console.log(`Color contrast report — ${path.basename(resolvedPath)}\n`);

for (const result of results) {
  if (result.error) {
    console.log(`✗ FAIL  ${result.name} — ${result.error}`);
    continue;
  }
  const status = result.pass ? '✓ PASS' : '✗ FAIL';
  console.log(
    `${status}  ${result.name} — ${result.ratio}:1 (needs ${result.required}:1) ` +
      `[${result.foreground} on ${result.background}]`
  );
}

const passCount = results.filter((r) => r.pass).length;
console.log(`\n${passCount}/${results.length} checks passed.`);

process.exit(hasFailure ? 1 : 0);
