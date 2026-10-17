// Renders raster brand assets from the vector sources in branding/.
//
//   cd scripts/brand && npm ci && node render.mjs
//
// Outputs (committed):
//   branding/generated/readme-banner.png     1600x520 README banner
//   branding/generated/social-preview.png    1280x640 GitHub social preview
//   branding/generated/play-icon-512.png     512x512 store icon
//   site/assets/icon-192.png, icon-512.png, favicon-32.png, apple-touch-icon.png
//
// The logo artwork itself is never altered: the lockup and icon SVGs are embedded verbatim
// (ids are namespaced to avoid collisions) on top of a procedurally drawn night street grid.
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '..', '..');
const brand = join(root, 'branding');
const out = join(brand, 'generated');
const siteAssets = join(root, 'site', 'assets');
const fontDir = join(root, 'core', 'designsystem', 'src', 'main', 'res', 'font');

const icon = readFileSync(join(brand, 'cloakrun-icon.svg'), 'utf8');
const logo = readFileSync(join(brand, 'cloakrun-logo.svg'), 'utf8');

/** Returns the inner markup of an SVG document with every id prefixed. */
function embed(svg, prefix) {
  const inner = svg.replace(/^[\s\S]*?<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '');
  return inner
    .replace(/id="([^"]+)"/g, `id="${prefix}-$1"`)
    .replace(/url\(#([^)]+)\)/g, `url(#${prefix}-$1)`)
    .replace(/href="#([^"]+)"/g, `href="#${prefix}-$1"`);
}

/** Small deterministic PRNG (mulberry32) so the artwork is identical on every run. */
function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** A night-map street grid: a jittered lattice of local streets plus a few glowing arterials. */
function streets(width, height, seed) {
  const r = rng(seed);
  const minor = [];
  const major = [];
  const step = 46;
  const angle = -14 * (Math.PI / 180);
  const cos = Math.cos(angle);
  const sin = Math.sin(angle);
  const rot = (x, y) => [x * cos - y * sin + width / 2, x * sin + y * cos + height / 2];
  const span = Math.max(width, height);
  for (let i = -span; i <= span; i += step) {
    const pts = [];
    for (let j = -span; j <= span; j += step * 2) {
      pts.push(rot(i + (r() - 0.5) * 14, j));
    }
    minor.push(pts);
    const pts2 = [];
    for (let j = -span; j <= span; j += step * 2) {
      pts2.push(rot(j, i + (r() - 0.5) * 14));
    }
    minor.push(pts2);
  }
  for (let k = 0; k < 5; k++) {
    const y0 = (r() - 0.5) * height * 1.2;
    const freq = 220 + r() * 160;
    const amp = 30 + r() * 40;
    const pts = [];
    for (let x = -span; x <= span; x += 24) {
      pts.push(rot(x, y0 + Math.sin(x / freq + k * 1.7) * amp));
    }
    major.push(pts);
  }
  const d = (pts) => 'M' + pts.map(([x, y]) => `${x.toFixed(1)} ${y.toFixed(1)}`).join(' L');
  return { minor: minor.map(d), major: major.map(d) };
}

function nightBackdrop(width, height, seed, focusX, focusY) {
  const { minor, major } = streets(width, height, seed);
  return `
  <defs>
    <linearGradient id="bn-bg" x1="0" y1="0" x2="${width}" y2="${height}" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#0D0830"/><stop offset="1" stop-color="#05040F"/>
    </linearGradient>
    <radialGradient id="bn-aura" cx="${focusX}" cy="${focusY}" r="${width * 0.55}" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#7C5CFF" stop-opacity="0.35"/>
      <stop offset="0.6" stop-color="#3B2BB8" stop-opacity="0.10"/>
      <stop offset="1" stop-color="#3B2BB8" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="bn-fade" cx="${focusX}" cy="${focusY}" r="${width * 0.62}" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#fff" stop-opacity="1"/>
      <stop offset="1" stop-color="#fff" stop-opacity="0.15"/>
    </radialGradient>
    <mask id="bn-mask"><rect width="${width}" height="${height}" fill="url(#bn-fade)"/></mask>
    <filter id="bn-glow" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="5"/></filter>
    <filter id="bn-shadow" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="28"/></filter>
  </defs>
  <rect width="${width}" height="${height}" fill="url(#bn-bg)"/>
  <g mask="url(#bn-mask)" fill="none" stroke-linecap="round" stroke-linejoin="round">
    <g stroke="#7C5CFF" stroke-opacity="0.16" stroke-width="1.4">${minor.map((p) => `<path d="${p}"/>`).join('')}</g>
    <g stroke="#5CF2FF" stroke-opacity="0.35" stroke-width="7" filter="url(#bn-glow)">${major.map((p) => `<path d="${p}"/>`).join('')}</g>
    <g stroke="#9DF8FF" stroke-opacity="0.55" stroke-width="1.8">${major.map((p) => `<path d="${p}"/>`).join('')}</g>
  </g>
  <rect width="${width}" height="${height}" fill="url(#bn-aura)"/>
  <g fill="none" stroke="#FFFFFF">
    <circle cx="${focusX}" cy="${focusY}" r="${height * 0.42}" stroke-opacity="0.07" stroke-width="2"/>
    <circle cx="${focusX}" cy="${focusY}" r="${height * 0.62}" stroke-opacity="0.05" stroke-width="2"/>
    <circle cx="${focusX}" cy="${focusY}" r="${height * 0.84}" stroke-opacity="0.035" stroke-width="2"/>
  </g>`;
}

function lockup(x, y, width) {
  const scale = width / 1700;
  const h = 620 * scale;
  return `
  <rect x="${x + 30 * scale}" y="${y + 60 * scale}" width="${width - 60 * scale}" height="${h - 60 * scale}" rx="${76 * scale}"
        fill="#3B2BB8" opacity="0.55" filter="url(#bn-shadow)"/>
  <g transform="translate(${x} ${y}) scale(${scale})">${embed(logo, 'lk')}</g>`;
}

function document(width, height, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${body}</svg>`;
}

function banner() {
  const w = 1600, h = 520;
  const lw = 1180;
  const lx = (w - lw) / 2;
  const ly = (h - 620 * (lw / 1700)) / 2;
  return document(w, h, nightBackdrop(w, h, 7, w / 2, h / 2) + lockup(lx, ly, lw));
}

function socialPreview() {
  const w = 1280, h = 640;
  const lw = 1000;
  const lh = 620 * (lw / 1700);
  const lx = (w - lw) / 2;
  const ly = 92;
  const text = `
  <text x="${w / 2}" y="${ly + lh + 70}" text-anchor="middle" font-family="Inter" font-weight="500" font-size="34"
        fill="#C9BDFF" letter-spacing="0.5">Premium real-world hide and seek</text>
  <text x="${w / 2}" y="${h - 40}" text-anchor="middle" font-family="Inter" font-weight="600" font-size="20"
        fill="#5CF2FF" fill-opacity="0.85" letter-spacing="3">MADE BY ARNAV GAMES</text>`;
  return document(w, h, nightBackdrop(w, h, 11, w / 2, ly + lh / 2) + lockup(lx, ly, lw) + text);
}

function iconOnly(size) {
  return document(size, size, `<g transform="scale(${size / 1024})">${embed(icon, 'ic')}</g>`);
}

const fontFiles = existsSync(fontDir)
  ? ['inter_variable.ttf', 'sora_variable.ttf'].map((f) => join(fontDir, f)).filter(existsSync)
  : [];

function render(svg, file, width) {
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: width },
    font: { fontFiles, loadSystemFonts: false, defaultFontFamily: 'Inter' },
    shapeRendering: 2,
    textRendering: 1,
  });
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, resvg.render().asPng());
  console.log('wrote', file.replace(root, '.'));
}

mkdirSync(out, { recursive: true });
writeFileSync(join(out, 'readme-banner.svg'), banner());
writeFileSync(join(out, 'social-preview.svg'), socialPreview());
render(banner(), join(out, 'readme-banner.png'), 1600);
render(socialPreview(), join(out, 'social-preview.png'), 1280);
render(iconOnly(1024), join(out, 'play-icon-512.png'), 512);
render(iconOnly(1024), join(siteAssets, 'icon-512.png'), 512);
render(iconOnly(1024), join(siteAssets, 'icon-192.png'), 192);
render(iconOnly(1024), join(siteAssets, 'apple-touch-icon.png'), 180);
render(iconOnly(1024), join(siteAssets, 'favicon-32.png'), 32);
