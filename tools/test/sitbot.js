// Minimal headless Minecraft client used to test the Sit mod end to end (Node built-ins only, offline-mode servers).
// It joins, answers keep-alives, and on request sends "use item on" (right click) at a block position.
//
// Usage: node sitbot.js <host> <port> <name> <protocol> <idsFile>
// stdin commands:  use <x> <y> <z> [face]   |  quit
// idsFile (json):  { hasConfig, ids: { cb_keep_alive, cb_keep_alive_config, sb_keep_alive, sb_keep_alive_config, sb_use_item_on,
//                    cb_finish_config, sb_finish_config, cb_known_packs, sb_known_packs, cb_ping, sb_pong, cb_ping_config, sb_pong_config } }
const net = require('net');
const zlib = require('zlib');
const fs = require('fs');
const crypto = require('crypto');

const [host, portArg, name, protoArg, idsFile] = process.argv.slice(2);
const port = +portArg, protocol = +protoArg;
const cfg = JSON.parse(fs.readFileSync(idsFile, 'utf8'));
const ids = cfg.ids;

const varint = (n) => { const out = []; n >>>= 0; do { let b = n & 0x7f; n >>>= 7; if (n) b |= 0x80; out.push(b); } while (n); return Buffer.from(out); };
const str = (s) => { const b = Buffer.from(s, 'utf8'); return Buffer.concat([varint(b.length), b]); };
const ushort = (n) => { const b = Buffer.alloc(2); b.writeUInt16BE(n); return b; };
function readVarint(buf, off) { let r = 0, shift = 0, b; do { if (off >= buf.length) return null; b = buf[off++]; r |= (b & 0x7f) << shift; shift += 7; } while (b & 0x80); return [r, off]; }

let watchUntil = 0;
let state = 'login', compression = -1, joined = false, buffer = Buffer.alloc(0);
const sock = net.connect(port, host);

function send(id, payload) {
  const body = Buffer.concat([varint(id), payload || Buffer.alloc(0)]);
  let frame;
  if (compression >= 0) {
    frame = body.length >= compression ? Buffer.concat([varint(body.length), zlib.deflateSync(body)]) : Buffer.concat([varint(0), body]);
  } else frame = body;
  sock.write(Buffer.concat([varint(frame.length), frame]));
}
const log = (...a) => console.log('[bot]', ...a);

sock.on('connect', () => {
  send(0, Buffer.concat([varint(protocol), str(host), ushort(port), varint(2)]));
  const uuid = crypto.createHash('md5').update('OfflinePlayer:' + name).digest();
  uuid[6] = (uuid[6] & 0x0f) | 0x30; uuid[8] = (uuid[8] & 0x3f) | 0x80;
  let login;
  if (protocol >= 764) login = Buffer.concat([str(name), uuid]);
  else if (protocol >= 761) login = Buffer.concat([str(name), Buffer.from([1]), uuid]);
  else if (protocol === 760) login = Buffer.concat([str(name), Buffer.from([0, 0])]);
  else if (protocol === 759) login = Buffer.concat([str(name), Buffer.from([0])]);
  else login = str(name);
  send(0, login);
});

sock.on('data', (d) => {
  buffer = Buffer.concat([buffer, d]);
  for (;;) {
    const l = readVarint(buffer, 0); if (!l) return;
    const [len, off] = l; if (buffer.length < off + len) return;
    let body = buffer.subarray(off, off + len); buffer = buffer.subarray(off + len);
    if (compression >= 0) {
      const [dl, o2] = readVarint(body, 0);
      body = dl === 0 ? body.subarray(o2) : zlib.inflateSync(body.subarray(o2));
    }
    const [id, o3] = readVarint(body, 0);
    try { handle(id, body.subarray(o3)); } catch (e) { log('handler error', e.message); }
  }
});
sock.on('close', () => { log('closed'); process.exit(0); });
sock.on('error', (e) => { log('error', e.message); process.exit(2); });

function printable(b) { return b.toString('utf8').replace(/[^\x20-\x7e]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 300); }

function handle(id, data) {
  if (state === 'login') {
    if (id === 0) { log('login disconnect:', printable(data)); }
    else if (id === 3) { compression = readVarint(data, 0)[0]; }
    else if (id === 4) { const [mid] = readVarint(data, 0); send(2, Buffer.concat([varint(mid), Buffer.from([0])])); } // login plugin: not understood
    else if (id === 2) {
      if (cfg.hasConfig) { send(3); state = 'config'; } else { state = 'play'; }
      log('login success');
    }
  } else if (state === 'config') {
    if (id === ids.cb_finish_config) { send(ids.sb_finish_config); state = 'play'; }
    else if (id === ids.cb_keep_alive_config) send(ids.sb_keep_alive_config, data.subarray(0, 8));
    else if (id === ids.cb_known_packs) send(ids.sb_known_packs, varint(0));
    else if (id === ids.cb_ping_config) send(ids.sb_pong_config, data.subarray(0, 4));
    else if (id === ids.cb_disconnect_config) log('config disconnect:', printable(data));
  } else {
    if (id === ids.cb_keep_alive) send(ids.sb_keep_alive, data.subarray(0, 8));
    else if (id === ids.cb_ping) send(ids.sb_pong, data.subarray(0, 4));
    else if (id === ids.cb_disconnect) { log('play disconnect:', printable(data)); }
    else if (id === ids.cb_login && !joined) {
      joined = true; log('joined');
      if (ids.sb_player_loaded >= 0 && ids.sb_player_loaded !== undefined) send(ids.sb_player_loaded);
    }
    else if (id === ids.cb_player_position && ids.sb_accept_teleportation !== undefined) {
      if (protocol >= 777) { // 26.x: the confirmation echoes the id, the position (3 doubles) and the rotation (2 floats)
        const [tid, o] = readVarint(data, 0);
        send(ids.sb_accept_teleportation, Buffer.concat([varint(tid), data.subarray(o, o + 24), data.subarray(o + 48, o + 56)]));
      }
      else {
        const tid = protocol >= 768 ? readVarint(data, 0)[0] : readVarint(data, 33)[0];
        send(ids.sb_accept_teleportation, varint(tid));
      }
    }
    if (watchUntil > Date.now()) log('  <- play packet', id, data.length);
  }
}

function useItemOn(x, y, z, face) {
  const pos = Buffer.alloc(8);
  pos.writeBigUInt64BE(((BigInt(x) & 0x3FFFFFFn) << 38n) | ((BigInt(z) & 0x3FFFFFFn) << 12n) | (BigInt(y) & 0xFFFn));
  const f = Buffer.alloc(12); f.writeFloatBE(0.5, 0); f.writeFloatBE(0.5, 4); f.writeFloatBE(0.5, 8);
  if (protocol < 477) { // 1.13: y sits between x and z in the packed position, hand follows the face
    const lp = Buffer.alloc(8); lp.writeBigUInt64BE(((BigInt(x) & 0x3FFFFFFn) << 38n) | ((BigInt(y) & 0xFFFn) << 26n) | (BigInt(z) & 0x3FFFFFFn));
    send(ids.sb_use_item_on, Buffer.concat([lp, varint(face), varint(0), f])); log('sent use_item_on (legacy)', x, y, z); return;
  }
  const parts = [varint(0), pos, varint(face), f, Buffer.from([0])]; // hand, position, face, cursor, insideBlock
  if (protocol >= 768) parts.push(Buffer.from([0])); // worldBorderHit (1.21.2+)
  if (protocol >= 759) parts.push(varint(1)); // sequence (1.19+)
  send(ids.sb_use_item_on, Buffer.concat(parts));
  log('sent use_item_on', x, y, z);
  watchUntil = Date.now() + 1200;
}

require('readline').createInterface({ input: process.stdin }).on('line', (line) => {
  const p = line.trim().split(/\s+/);
  if (p[0] === 'use') useItemOn(+p[1], +p[2], +p[3], p[4] === undefined ? 1 : +p[4]);
  else if (p[0] === 'quit') { sock.end(); setTimeout(() => process.exit(0), 200); }
});
