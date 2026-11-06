// Extracts the packet ids sitbot.js needs from a vanilla server jar older than 1.20.2 (no packets.json report exists there).
// It reads the registration order of net.minecraft.network.ConnectionProtocol from the bytecode.
// Usage: node oldids.js <server.jar> <mojang.txt> <out.json> <workDir>
const fs = require('fs'), path = require('path'), { execFileSync } = require('child_process');
const [jar0, mojFile, outFile, work] = process.argv.slice(2);
fs.mkdirSync(work, { recursive: true });

// 1. locate the real server jar (bundler jars wrap it)
let jar = jar0;
const listing = execFileSync('unzip', ['-Z1', jar0], { maxBuffer: 1 << 28 }).toString().split(/\r?\n/);
const inner = listing.find(n => /^META-INF\/versions\/[^/]+\/server-[^/]+\.jar$/.test(n));
if (inner) { execFileSync('unzip', ['-o', '-q', jar0, inner, '-d', work]); jar = path.join(work, inner); }

// 2. Mojang names <-> obfuscated names
const moj2obf = new Map(), obf2moj = new Map();
let cur = null; const fieldsObf = new Map();
for (const line of fs.readFileSync(mojFile, 'utf8').split(/\r?\n/)) {
  if (!line || line[0] === '#') continue;
  if (line[0] !== ' ') { const m = line.match(/^(\S+) -> (\S+):$/); if (m) { moj2obf.set(m[1], m[2]); obf2moj.set(m[2], m[1]); cur = m[1]; } }
  else if (cur === 'net.minecraft.network.protocol.PacketFlow') { const m = line.trim().match(/^\S+ ([A-Z_]+) -> (\S+)$/); if (m) fieldsObf.set(m[2], m[1]); }
}
const cp = moj2obf.get('net.minecraft.network.ConnectionProtocol');
const flowObf = moj2obf.get('net.minecraft.network.protocol.PacketFlow');
const dis = execFileSync('javap', ['-c', '-p', '-cp', jar, cp], { maxBuffer: 1 << 28 }).toString();

// 3. walk <clinit> in order; track state + direction; collect packet classes
const ids = {}; // `${state}/${flow}` -> [mojName...]
let state = 'HANDSHAKING', flow = 'CLIENTBOUND';
const body = dis.slice(dis.indexOf('static {};'));
for (const line of body.split(/\r?\n/)) {
  let m = line.match(/ldc(?:_w)?\s+#\d+\s+\/\/ String (HANDSHAKING|PLAY|STATUS|LOGIN|CONFIGURATION)$/);
  if (m) { state = m[1]; continue; }
  m = line.match(/getstatic\s+#\d+\s+\/\/ Field (\S+)\.(\S+):L(\S+);/);
  if (m && m[1].replace(/\//g, '.') === flowObf && fieldsObf.has(m[2])) { flow = fieldsObf.get(m[2]); continue; }
  m = line.match(/ldc(?:_w)?\s+#\d+\s+\/\/ class (\S+)$/);
  if (m) {
    const moj = obf2moj.get(m[1].replace(/\//g, '.'));
    if (moj && /\.(Clientbound|Serverbound)[\w$]+$/.test(moj) && moj.startsWith('net.minecraft.network.protocol.')) (ids[state + '/' + flow] = ids[state + '/' + flow] || []).push(moj.split('.').pop());
  }
}
const find = (state, flow, cls) => { const a = ids[state + '/' + flow] || []; return a.indexOf(cls); };
const hasConfig = !!ids['CONFIGURATION/CLIENTBOUND'];
const out = { hasConfig, ids: {
  cb_keep_alive: find('PLAY', 'CLIENTBOUND', 'ClientboundKeepAlivePacket'),
  sb_keep_alive: find('PLAY', 'SERVERBOUND', 'ServerboundKeepAlivePacket'),
  cb_ping: find('PLAY', 'CLIENTBOUND', 'ClientboundPingPacket'),
  sb_pong: find('PLAY', 'SERVERBOUND', 'ServerboundPongPacket'),
  cb_disconnect: find('PLAY', 'CLIENTBOUND', 'ClientboundDisconnectPacket'),
  cb_login: find('PLAY', 'CLIENTBOUND', 'ClientboundLoginPacket'),
  cb_player_position: find('PLAY', 'CLIENTBOUND', 'ClientboundPlayerPositionPacket'),
  sb_accept_teleportation: find('PLAY', 'SERVERBOUND', 'ServerboundAcceptTeleportationPacket'),
  sb_use_item_on: find('PLAY', 'SERVERBOUND', 'ServerboundUseItemOnPacket'),
  cb_finish_config: find('CONFIGURATION', 'CLIENTBOUND', 'ClientboundFinishConfigurationPacket'),
  sb_finish_config: find('CONFIGURATION', 'SERVERBOUND', 'ServerboundFinishConfigurationPacket'),
  cb_keep_alive_config: find('CONFIGURATION', 'CLIENTBOUND', 'ClientboundKeepAlivePacket'),
  sb_keep_alive_config: find('CONFIGURATION', 'SERVERBOUND', 'ServerboundKeepAlivePacket'),
  cb_ping_config: find('CONFIGURATION', 'CLIENTBOUND', 'ClientboundPingPacket'),
  sb_pong_config: find('CONFIGURATION', 'SERVERBOUND', 'ServerboundPongPacket'),
  cb_disconnect_config: find('CONFIGURATION', 'CLIENTBOUND', 'ClientboundDisconnectPacket'),
} };
for (const k of Object.keys(out.ids)) if (out.ids[k] < 0) delete out.ids[k];
if (process.env.DUMP) console.log((ids['PLAY/SERVERBOUND'] || []).map((n, i) => i + ':' + n).join(' | '));
fs.writeFileSync(outFile, JSON.stringify(out, null, 1));
console.log(Object.keys(ids).map(k => k + '=' + ids[k].length).join(' '), JSON.stringify(out.ids));
