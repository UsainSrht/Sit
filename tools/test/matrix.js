// Runs the end-to-end test for each "<loader>:<mc>" combo with the freshly built jar.
// Usage: node matrix.js <rootDir> <jar> <combo> [<combo> ...]     e.g. node matrix.js C:\tmp\t\srv target\sit-1.8.0.jar fabric:1.21.4 forge:1.20.1
const fs = require('fs'), path = require('path'), { execFileSync, spawnSync } = require('child_process');
const [root, jar, ...combos] = process.argv.slice(2);
const results = [];
for (const combo of combos) {
  const [loader, mc] = combo.split(':');
  const t0 = Date.now();
  let info;
  try {
    const out = execFileSync('node', [path.join(__dirname, 'prep.js'), loader, mc, root], { encoding: 'utf8', timeout: 1200000, stdio: ['ignore', 'pipe', 'inherit'] });
    info = JSON.parse(out.trim().split('\n').pop());
  } catch (e) { results.push({ combo, status: 'PREP-FAIL', detail: String(e.message).slice(0, 200) }); console.log(combo, 'PREP-FAIL'); continue; }
  const isPlugin = ['paper', 'folia', 'purpur'].includes(loader);
  for (const d of ['ops.json', 'usercache.json', 'world', 'world_nether', 'world_the_end', 'config/Sit', 'plugins/Sit', 'crash-reports', 'logs']) fs.rmSync(path.join(info.dir, d), { recursive: true, force: true });
  const dest = path.join(info.dir, isPlugin ? 'plugins' : 'mods');
  fs.mkdirSync(dest, { recursive: true });
  for (const f of fs.readdirSync(dest)) if (/^sit-/.test(f)) fs.rmSync(path.join(dest, f));
  fs.copyFileSync(jar, path.join(dest, path.basename(jar)));
  const r = spawnSync('node', [path.join(__dirname, 'runtest.js'), '--dir', info.dir, '--java', info.java, '--args', info.args, '--ids', info.idsFile, '--protocol', String(info.protocol), '--timeout', '420', '--seat', isPlugin ? '@e[type=pig,x=0,y=119,z=1,distance=..5]' : '@e[tag=sit_seat]', '--port', process.env.SIT_PORT || '25577'],
    { encoding: 'utf8', timeout: 600000, maxBuffer: 1 << 26 });
  const text = (r.stdout || '') + (r.stderr || '');
  fs.mkdirSync(path.join(root, '..', 'results'), { recursive: true });
  fs.writeFileSync(path.join(root, '..', 'results', combo.replace(':', '-') + '.txt'), text);
  const passed = (text.match(/^PASS /gm) || []).length, failed = (text.match(/^FAIL /gm) || []).length;
  const status = /RESULT: ALL PASSED/.test(text) ? 'PASS' : 'FAIL';
  const firstFail = (text.match(/^FAIL .*$/m) || [''])[0].slice(0, 160);
  results.push({ combo, status, passed, failed, secs: Math.round((Date.now() - t0) / 1000), detail: status === 'PASS' ? '' : firstFail || text.slice(-300).replace(/\s+/g, ' ') });
  console.log(combo.padEnd(18), status.padEnd(5), `${passed} ok / ${failed} failed`, `${Math.round((Date.now() - t0) / 1000)}s`, status === 'PASS' ? '' : '\n    ' + results[results.length - 1].detail);
}
console.log('\nSUMMARY\n' + results.map(r => `${r.combo.padEnd(18)} ${r.status}`).join('\n'));
