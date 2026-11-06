// Provisions a test server for <loader> <mcVersion> under <root>/<loader>-<mc>.  Prints a JSON description on the last line.
// Usage: node prep.js <fabric|forge|neoforge> <mc> <root> [<loaderVersionOverride>]
const fs = require('fs'), path = require('path'), { execFileSync } = require('child_process');
const [loader, mc, root, override] = process.argv.slice(2);
const T = path.resolve(root, '..');               // /tmp/t
const dir = path.join(root, `${loader}-${mc}`);
const JAVA = {
  8: 'C:\\Program Files\\Java\\jre1.8.0_481\\bin\\java.exe',
  17: fs.readdirSync(path.join(T, 'jdk')).filter(n => n.startsWith('jdk-17')).map(n => path.join(T, 'jdk', n, 'bin', 'java.exe'))[0],
  21: 'C:\\Program Files\\Microsoft\\jdk-21.0.11.10-hotspot\\bin\\java.exe',
  25: 'C:\\Program Files\\Microsoft\\jdk-25.0.2.10-hotspot\\bin\\java.exe',
};
const ver = mc.split('.').map(Number);
const javaMajor = ver[0] >= 26 ? 25 : (ver[1] <= 16 ? 8 : (ver[1] < 20 || (ver[1] === 20 && ver[2] < 5)) ? 17 : 21);
const java = JAVA[javaMajor];
const mcMinor = ver[1], mcPatch = ver[2] || 0;

async function get(url, as = 'buffer') {
  const r = await fetch(url, { headers: { 'User-Agent': 'sit-mod-tests/1.0' } });
  if (!r.ok) throw new Error(r.status + ' ' + url);
  return as === 'json' ? r.json() : as === 'text' ? r.text() : Buffer.from(await r.arrayBuffer());
}

async function vanillaIds() {
  const vdir = path.join(T, 'vanilla', mc);
  const idsFile = path.join(vdir, 'ids.json');
  const manual = path.join(__dirname, 'ids', mc + '.json');
  fs.mkdirSync(vdir, { recursive: true });
  if (fs.existsSync(manual)) { fs.copyFileSync(manual, idsFile); return { idsFile, protocol: JSON.parse(fs.readFileSync(manual, 'utf8')).protocol }; }
  const jar = path.join(vdir, 'server.jar');
  fs.mkdirSync(vdir, { recursive: true });
  if (!fs.existsSync(jar)) {
    const manifest = await get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', 'json');
    const vj = await get(manifest.versions.find(v => v.id === mc).url, 'json');
    fs.writeFileSync(jar, await get(vj.downloads.server.url));
  }
  const vjson = JSON.parse(execFileSync('unzip', ['-p', jar, 'version.json'], { maxBuffer: 1 << 26 }).toString());
  if (!fs.existsSync(idsFile)) {
    if (vjson.protocol_version >= 764 && vjson.protocol_version < 0x40000000 && !(mcMinor === 20 && mcPatch <= 4)) {
      // 1.20.2+ : packets.json exists
      if (!fs.existsSync(path.join(vdir, 'out/reports/packets.json'))) {
        execFileSync(JAVA[javaMajor === 8 ? 17 : javaMajor], ['-DbundlerMainClass=net.minecraft.data.Main', '-jar', jar, '--reports', '--output', path.join(vdir, 'out')], { stdio: 'ignore' });
      }
      execFileSync('node', [path.join(__dirname, 'mkids.js'), path.join(vdir, 'out/reports/packets.json'), idsFile], { stdio: 'ignore' });
    } else {
      execFileSync('node', [path.join(__dirname, 'oldids.js'), jar, path.join(T, 'maps', mc, 'mojang.txt'), idsFile, path.join(vdir, 'work')], { stdio: 'ignore' });
    }
  }
  const protocol = vjson.protocol_version;
  // snapshots / 26.x use a data version in the high bit range for protocol: keep as is, the bot sends it verbatim
  return { idsFile, protocol };
}

(async () => {
  fs.mkdirSync(dir, { recursive: true });
  fs.mkdirSync(path.join(dir, 'mods'), { recursive: true });
  fs.writeFileSync(path.join(dir, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(path.join(dir, 'server.properties'), 'online-mode=false\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\nserver-port=' + (process.env.SIT_PORT || 25577) + '\nenable-rcon=false\nwhite-list=false\nenforce-whitelist=false\nallow-flight=true\n');
  let args;
  const marker = path.join(dir, '.prepared');
  if (!fs.existsSync(marker)) {
    if (loader === 'paper' || loader === 'folia' || loader === 'purpur') {
      fs.mkdirSync(path.join(dir, 'plugins'), { recursive: true });
      let url;
      if (loader === 'purpur') url = `https://api.purpurmc.org/v2/purpur/${mc}/latest/download`;
      else { const b = await get(`https://fill.papermc.io/v3/projects/${loader}/versions/${mc}/builds/latest`, 'json'); url = b.downloads['server:default'].url; }
      fs.writeFileSync(path.join(dir, 'server.jar'), await get(url));
    } else if (loader === 'fabric') {
      const loaders = await get('https://meta.fabricmc.net/v2/versions/loader', 'json');
      const installers = await get('https://meta.fabricmc.net/v2/versions/installer', 'json');
      const lv = override || loaders.find(x => x.stable).version, iv = installers.find(x => x.stable).version;
      fs.writeFileSync(path.join(dir, 'fabric.jar'), await get(`https://meta.fabricmc.net/v2/versions/loader/${mc}/${lv}/${iv}/server/jar`));
      const q = `https://api.modrinth.com/v2/project/fabric-api/version?game_versions=${encodeURIComponent(JSON.stringify([mc]))}&loaders=${encodeURIComponent('["fabric"]')}`;
      const versions = await get(q, 'json');
      if (!versions.length) throw new Error('no fabric-api for ' + mc);
      const file = versions[0].files.find(f => f.primary) || versions[0].files[0];
      fs.writeFileSync(path.join(dir, 'mods', 'fabric-api.jar'), await get(file.url));
    } else {
      let v = override, url, mavenBase;
      if (loader === 'forge') {
        if (!v) { const promos = (await get('https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json', 'json')).promos; v = promos[mc + '-latest'] || promos[mc + '-recommended']; }
        mavenBase = `https://maven.minecraftforge.net/net/minecraftforge/forge/${mc}-${v}`;
        url = `${mavenBase}/forge-${mc}-${v}-installer.jar`;
      } else {
        if (!v) {
          const all = (await get('https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge', 'json')).versions;
          const prefix = mc.startsWith('26.') ? mc + '.' : mc.replace(/^1\./, '') + '.';
          const cands = all.filter(x => x.startsWith(prefix));
          v = cands.filter(x => !x.includes('beta')).pop() || cands.pop();
        }
        url = `https://maven.neoforged.net/releases/net/neoforged/neoforge/${v}/neoforge-${v}-installer.jar`;
      }
      fs.writeFileSync(path.join(dir, 'installer.jar'), await get(url));
      execFileSync(JAVA[javaMajor === 8 ? 8 : javaMajor], ['-jar', 'installer.jar', '--installServer'], { cwd: dir, stdio: 'ignore', timeout: 900000 });
      fs.writeFileSync(path.join(dir, '.loaderversion'), v);
    }
    fs.writeFileSync(marker, 'ok');
  }
  // launch arguments
  if (loader === 'paper' || loader === 'folia' || loader === 'purpur') args = ['-Xmx1536M', '-Dsit.debug=true', '-jar', 'server.jar', 'nogui'];
  else if (loader === 'fabric') args = ['-Xmx1536M', '-Dsit.debug=true', '-jar', 'fabric.jar', 'nogui'];
  else {
    const v = fs.readFileSync(path.join(dir, '.loaderversion'), 'utf8').trim();
    const sub = loader === 'forge' ? `net/minecraftforge/forge/${mc}-${v}` : `net/neoforged/neoforge/${v}`;
    const argFile = path.join(dir, 'libraries', sub, 'win_args.txt');
    if (fs.existsSync(argFile)) args = ['-Xmx1536M', '-Dsit.debug=true', `@libraries/${sub}/win_args.txt`, 'nogui'];
    else {
      const jar = fs.readdirSync(dir).find(n => /^forge-.*\.jar$/.test(n) && !n.includes('installer'));
      args = ['-Xmx1536M', '-Dsit.debug=true', '-jar', jar, 'nogui'];
    }
  }
  const { idsFile, protocol } = await vanillaIds();
  console.log(JSON.stringify({ dir, java, args: args.join(' '), idsFile, protocol, loaderVersion: fs.existsSync(path.join(dir, '.loaderversion')) ? fs.readFileSync(path.join(dir, '.loaderversion'), 'utf8').trim() : '' }));
})().catch(e => { console.error('PREP FAILED', e.message); process.exit(1); });
