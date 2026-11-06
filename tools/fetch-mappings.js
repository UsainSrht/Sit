// Downloads Mojang (proguard), Fabric intermediary and Forge MCPConfig (SRG) mappings for MC 1.16.5..1.21.11
// Usage: node fetch-mappings.js <outDir>
const fs = require('fs'), path = require('path'), { execSync } = require('child_process');
const out = process.argv[2];
const VERSIONS = ['1.14.4','1.15.2','1.16.5','1.17','1.17.1','1.18','1.18.1','1.18.2','1.19','1.19.1','1.19.2','1.19.3','1.19.4',
 '1.20','1.20.1','1.20.2','1.20.3','1.20.4','1.20.5','1.20.6','1.21','1.21.1','1.21.2','1.21.3','1.21.4','1.21.5',
 '1.21.6','1.21.7','1.21.8','1.21.9','1.21.10','1.21.11'];
const get = async (u) => { const r = await fetch(u); if (!r.ok) throw new Error(r.status + ' ' + u); return r; };
(async () => {
  fs.mkdirSync(out, { recursive: true });
  const manifest = await (await get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')).json();
  const forgeMeta = await (await get('https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/maven-metadata.xml')).text();
  const mcpVers = [...forgeMeta.matchAll(/<version>([^<]+)<\/version>/g)].map(m => m[1]);
  for (const v of VERSIONS) {
    const dir = path.join(out, v); fs.mkdirSync(dir, { recursive: true });
    try {
      if (!fs.existsSync(dir + '/mojang.txt')) {
        const meta = manifest.versions.find(x => x.id === v);
        const vj = await (await get(meta.url)).json();
        const mm = vj.downloads.client_mappings;
        fs.writeFileSync(dir + '/mojang.txt', Buffer.from(await (await get(mm.url)).arrayBuffer()));
      }
      if (!fs.existsSync(dir + '/intermediary.tiny')) {
        const jar = dir + '/interm.jar';
        fs.writeFileSync(jar, Buffer.from(await (await get(`https://maven.fabricmc.net/net/fabricmc/intermediary/${v}/intermediary-${v}-v2.jar`)).arrayBuffer()));
        execSync(`unzip -o -q "${jar}" mappings/mappings.tiny -d "${dir}/x"`);
        fs.renameSync(dir + '/x/mappings/mappings.tiny', dir + '/intermediary.tiny');
        fs.rmSync(dir + '/x', { recursive: true }); fs.rmSync(jar);
      }
      if (!fs.existsSync(dir + '/joined.tsrg')) {
        const cands = mcpVers.filter(x => x.startsWith(v + '-'));
        if (cands.length) {
          const mv = cands[cands.length - 1];
          const zip = dir + '/mcp.zip';
          fs.writeFileSync(zip, Buffer.from(await (await get(`https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/${mv}/mcp_config-${mv}.zip`)).arrayBuffer()));
          try { execSync(`unzip -o -q "${zip}" config/joined.tsrg -d "${dir}/x"`); fs.renameSync(dir + '/x/config/joined.tsrg', dir + '/joined.tsrg'); }
          catch (e) { try { execSync(`unzip -o -q "${zip}" joined.tsrg -d "${dir}/x"`); fs.renameSync(dir + '/x/joined.tsrg', dir + '/joined.tsrg'); } catch (e2) { console.log('  no tsrg in', mv); } }
          fs.rmSync(dir + '/x', { recursive: true, force: true }); fs.rmSync(zip);
        } else console.log('  no mcp_config for', v);
      }
      console.log('ok', v, fs.readdirSync(dir).join(','));
    } catch (e) { console.log('FAIL', v, e.message); }
  }
})();
