// Resolves api.spec against Mojang/intermediary/SRG mappings of every downloaded version.
// Usage: node gen-mappings.js <mapsDir> <api.spec> <out.txt>
const fs = require('fs');
const [mapsDir, specFile, outFile] = process.argv.slice(2);
// The mod supports Minecraft 1.16.5 and newer; older folders (kept for test tooling) are ignored.
const atLeast1165 = (v) => { const p = v.split('.').map(Number); return p[0] > 1 || p[1] > 16 || (p[1] === 16 && (p[2] || 0) >= 5); };
const versions = fs.readdirSync(mapsDir).filter(v => fs.existsSync(`${mapsDir}/${v}/mojang.txt`) && atLeast1165(v));
versions.sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));

const spec = { classes: {}, items: [] };
function splitId(s) { const i = s.indexOf('.'); return [s.slice(0, i), s.slice(i + 1)]; }
for (const raw of fs.readFileSync(specFile, 'utf8').split(/\r?\n/)) {
  const l = raw.trim(); if (!l || l.startsWith('#')) continue;
  const p = l.split(/\s+/);
  if (p[0] === 'class') spec.classes[p[1]] = p[2].split('|');
  else if (p[0] === 'method') { const [c, id] = splitId(p[1]); spec.items.push({ kind: 'M', owner: c, id, argc: +p[2], decl: p[3].split('|'), names: p[4].split('|') }); }
  else if (p[0] === 'field') { const [c, id] = splitId(p[1]); spec.items.push({ kind: 'F', owner: c, id, decl: p[2].split('|'), names: p[3].split('|') }); }
}

function parseMojang(file) {
  const classes = new Map();
  let cur = null;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    if (!line || line[0] === '#') continue;
    if (line[0] !== ' ') {
      const m = line.match(/^(\S+) -> (\S+):$/); if (!m) continue;
      cur = { moj: m[1], obf: m[2], members: [] }; classes.set(m[1], cur);
    } else if (cur) {
      const t = line.trim().replace(/^\d+:\d+:/, '');
      const m = t.match(/^(\S+) ([^\s(]+)(\(([^)]*)\))? -> (\S+)$/); if (!m) continue;
      if (m[3] !== undefined) cur.members.push({ kind: 'M', ret: m[1], name: m[2], args: m[4] ? m[4].split(',') : [], obf: m[5] });
      else cur.members.push({ kind: 'F', ret: m[1], name: m[2], obf: m[5] });
    }
  }
  return { classes };
}
const PRIM = { int: 'I', long: 'J', boolean: 'Z', byte: 'B', short: 'S', char: 'C', float: 'F', double: 'D', void: 'V' };
function desc(t, moj) {
  let dims = 0; while (t.endsWith('[]')) { dims++; t = t.slice(0, -2); }
  const d = PRIM[t] || ('L' + (moj.classes.has(t) ? moj.classes.get(t).obf : t).replace(/\./g, '/') + ';');
  return '['.repeat(dims) + d;
}
function methodDesc(m, moj) { return '(' + m.args.map(a => desc(a, moj)).join('') + ')' + desc(m.ret, moj); }

function parseTiny(file) {
  const cls = new Map(); let cur = null;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const p = line.split('\t');
    if (p[0] === 'c') { cur = { name: p[2].replace(/\//g, '.'), m: new Map(), f: new Map() }; cls.set(p[1].split('/').join('.'), cur); }
    else if (p[0] === '' && cur) {
      if (p[1] === 'm') cur.m.set(p[3] + p[2], p[4]); else if (p[1] === 'f') cur.f.set(p[3], p[4]);
    }
  }
  return cls;
}
function parseTsrg(file) {
  const cls = new Map(); let cur = null;
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
  const v2 = lines[0].startsWith('tsrg2');
  for (const line of lines) {
    if (!line || line.startsWith('tsrg2')) continue;
    if (line[0] !== '\t') { const p = line.split(' '); cur = { name: p[1].replace(/\//g, '.'), m: new Map(), f: new Map() }; cls.set(p[0].split('/').join('.'), cur); }
    else if (line[1] !== '\t' && cur) {
      const p = line.slice(1).split(' ');
      if (p.length >= 3 && p[1].startsWith('(')) cur.m.set(p[0] + p[1], p[2]);
      else if (p[0] !== 'static') cur.f.set(p[0], p[1]);
    }
  }
  return { cls, v2 };
}

const out = new Map();
const add = (key, vals) => { if (!out.has(key)) out.set(key, new Set()); for (const v of vals) if (v) out.get(key).add(v); };
const report = {}, noI = {}, noS = {};
for (const v of versions) {
  const dir = `${mapsDir}/${v}`;
  const moj = parseMojang(dir + '/mojang.txt');
  const tiny = parseTiny(dir + '/intermediary.tiny');
  const tsrg = fs.existsSync(dir + '/joined.tsrg') ? parseTsrg(dir + '/joined.tsrg') : null;
  for (const k of Object.keys(spec.classes)) {
    const names = [];
    let found = false;
    for (const mn of spec.classes[k]) {
      names.push(mn);
      const mc = moj.classes.get(mn); if (!mc) continue;
      found = true;
      const t = tiny.get(mc.obf); if (t) names.push(t.name);
      if (tsrg && !tsrg.v2) { const s = tsrg.cls.get(mc.obf); if (s) names.push(s.name); }
    }
    add('C:' + k, names);
    (report['C:' + k] = report['C:' + k] || []).push(found ? v : '-' + v);
  }
  for (const it of spec.items) {
    const key = (it.kind === 'M' ? 'M:' : 'F:') + it.owner + '.' + it.id;
    add(key, it.names); let hit = false, hitI = false, hitS = false;
    for (const dk of it.decl) {
      for (const cn of spec.classes[dk] || []) {
        const mc = moj.classes.get(cn); if (!mc) continue;
        for (const m of mc.members) {
          if (m.kind !== it.kind || !it.names.includes(m.name)) continue;
          if (it.kind === 'M' && m.args.length !== it.argc) continue;
          hit = true;
          const mk = it.kind === 'M' ? m.obf + methodDesc(m, moj) : m.obf;
          const ti = tiny.get(mc.obf);
          if (ti) { const r = it.kind === 'M' ? ti.m.get(mk) : ti.f.get(mk); if (r) hitI = true; add(key, [r]); }
          if (tsrg) { const ts = tsrg.cls.get(mc.obf); if (ts) { const r = it.kind === 'M' ? ts.m.get(mk) : ts.f.get(mk); if (r) hitS = true; add(key, [r]); } }
        }
      }
    }
    (report[key] = report[key] || []).push(hit ? v : '-' + v);
    if (hit && !hitI) (noI[key] = noI[key] || []).push(v);
    if (hit && tsrg && !hitS) (noS[key] = noS[key] || []).push(v);
  }
}
fs.writeFileSync(outFile, [...out.entries()].sort().map(([k, s]) => k + '=' + [...s].join(',')).join('\n') + '\n');
function range(miss, all) {
  const idx = miss.map(m => all.indexOf(m)).sort((a, b) => a - b); const r = []; let s = idx[0], p = idx[0];
  for (let i = 1; i <= idx.length; i++) { if (idx[i] !== p + 1) { r.push(s === p ? all[s] : all[s] + '..' + all[p]); s = idx[i]; } p = idx[i]; }
  return r.join(' ');
}
for (const [k, arr] of Object.entries(report)) {
  const miss = arr.filter(x => x.startsWith('-')).map(x => x.slice(1));
  console.log(k.padEnd(42), miss.length === 0 ? 'all' : miss.length === arr.length ? 'NONE' : 'missing: ' + range(miss, versions));
}
for (const [k, a] of Object.entries(noI)) console.log('NO-INTERMEDIARY', k, range(a, versions));
for (const [k, a] of Object.entries(noS)) console.log('NO-SRG', k, range(a, versions));
