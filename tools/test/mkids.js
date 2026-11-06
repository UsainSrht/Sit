// Builds the packet id file for sitbot.js from a vanilla server's data report (1.20.2+).
// Usage: node mkids.js <packets.json> <out.json>
const fs = require('fs');
const j = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const id = (state, dir, name) => { const p = j[state] && j[state][dir] && j[state][dir]['minecraft:' + name]; return p ? p.protocol_id : -1; };
const out = {
  hasConfig: true,
  ids: {
    cb_keep_alive: id('play', 'clientbound', 'keep_alive'),
    sb_keep_alive: id('play', 'serverbound', 'keep_alive'),
    cb_ping: id('play', 'clientbound', 'ping'),
    sb_pong: id('play', 'serverbound', 'pong'),
    cb_disconnect: id('play', 'clientbound', 'disconnect'),
    cb_login: id('play', 'clientbound', 'login'),
    cb_player_position: id('play', 'clientbound', 'player_position'),
    sb_accept_teleportation: id('play', 'serverbound', 'accept_teleportation'),
    sb_player_loaded: id('play', 'serverbound', 'player_loaded'),
    sb_use_item_on: id('play', 'serverbound', 'use_item_on'),
    cb_finish_config: id('configuration', 'clientbound', 'finish_configuration'),
    sb_finish_config: id('configuration', 'serverbound', 'finish_configuration'),
    cb_keep_alive_config: id('configuration', 'clientbound', 'keep_alive'),
    sb_keep_alive_config: id('configuration', 'serverbound', 'keep_alive'),
    cb_known_packs: id('configuration', 'clientbound', 'select_known_packs'),
    sb_known_packs: id('configuration', 'serverbound', 'select_known_packs'),
    cb_ping_config: id('configuration', 'clientbound', 'ping'),
    sb_pong_config: id('configuration', 'serverbound', 'pong'),
    cb_disconnect_config: id('configuration', 'clientbound', 'disconnect'),
  },
};
fs.writeFileSync(process.argv[3], JSON.stringify(out, null, 1));
console.log(JSON.stringify(out.ids));
