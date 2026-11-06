// Verifies api.spec against an unobfuscated Minecraft jar (26.x and newer) using javap.
// Usage: node check-unobf.js <jar> <api.spec>
const fs = require('fs'), { execFileSync } = require('child_process');
const [jar, specFile] = process.argv.slice(2);
const spec = { classes: {}, items: [] };
for (const raw of fs.readFileSync(specFile, 'utf8').split(/\r?\n/)) {
  const l = raw.trim(); if (!l || l.startsWith('#')) continue;
  const p = l.split(/\s+/);
  const id = (s) => s.slice(s.indexOf('.') + 1);
  if (p[0] === 'class') spec.classes[p[1]] = p[2].split('|');
  else if (p[0] === 'method') spec.items.push({ kind: 'M', key: p[1], argc: +p[2], decl: p[3].split('|'), names: p[4].split('|') });
  else if (p[0] === 'field') spec.items.push({ kind: 'F', key: p[1], decl: p[2].split('|'), names: p[3].split('|') });
}
const cache = {};
function members(cls) {
  if (cache[cls]) return cache[cls];
  let out = '';
  try { out = execFileSync('javap', ['-public', '-cp', jar, cls], { encoding: 'utf8', maxBuffer: 1 << 26, stdio: ['ignore', 'pipe', 'ignore'] }); } catch (e) { out = null; }
  return (cache[cls] = out);
}
function argc(params) { let d = 0, n = 0, any = false; for (const c of params) { if (c === '<') d++; else if (c === '>') d--; else if (c === ',' && d === 0) n++; if (!/\s/.test(c)) any = true; } return any ? n + 1 : 0; }
let bad = 0;
for (const [k, names] of Object.entries(spec.classes)) {
  if (!names.some(n => members(n) !== null)) { console.log('CLASS missing   ', k, names.join('|')); bad++; }
}
for (const it of spec.items) {
  let found = false;
  for (const dk of it.decl) for (const cn of spec.classes[dk] || []) {
    const text = members(cn); if (!text) continue;
    for (const line of text.split(/\r?\n/)) {
      for (const name of it.names) {
        if (it.kind === 'M') { const m = line.match(new RegExp('[ .]' + name.replace('$', '\\$') + '\\((.*)\\)')); if (m && argc(m[1]) === it.argc) found = true; }
        else if (new RegExp('[ .]' + name + ';').test(line)) found = true;
      }
    }
  }
  if (!found) { console.log('MEMBER missing  ', it.kind + ':' + it.key, '(' + it.names.join('|') + ')'); bad++; }
}
console.log(bad ? bad + ' spec entries not found' : 'all spec entries found');
