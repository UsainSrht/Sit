// End-to-end test: start a server, join sitbot, build a stair, right click it, verify the seat exists and is cleaned up.
// Usage: node runtest.js --dir <serverDir> --java <javaExe> --args "<jvm/server args>" --ids ids.json --protocol 769 [--ready Done] [--timeout 240]
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const arg = (n, d) => { const i = process.argv.indexOf('--' + n); return i > 0 ? process.argv[i + 1] : d; };
const dir = arg('dir'), java = arg('java', 'java'), sargs = arg('args').split(' ').filter(Boolean);
const ids = arg('ids'), protocol = arg('protocol'), ready = new RegExp(arg('ready', 'Done \\(')), timeout = +arg('timeout', 300) * 1000;
const SEAT = arg('seat', '@e[tag=sit_seat]');
const botFile = path.join(__dirname, 'sitbot.js');
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

let log = '';
const server = spawn(java, sargs, { cwd: dir, stdio: ['pipe', 'pipe', 'pipe'] });
server.stdout.on('data', d => { log += d; });
server.stderr.on('data', d => { log += d; });
const cmd = (c) => server.stdin.write(c + '\n');
const waitFor = async (re, ms, from = 0) => { const end = Date.now() + ms; while (Date.now() < end) { if (re.test(log.slice(from))) return true; await sleep(250); } return false; };
const result = { steps: [] };
const step = (name, ok, info) => { result.steps.push({ name, ok, info }); console.log((ok ? 'PASS ' : 'FAIL ') + name + (info ? ' :: ' + info : '')); return ok; };
const slice = async (c, ms = 2500) => { const from = log.length; cmd(c); await sleep(ms); return log.slice(from); };

(async () => {
  const kill = setTimeout(() => { console.log('TIMEOUT'); server.kill('SIGKILL'); process.exit(3); }, timeout);
  if (!step('server started', await waitFor(ready, timeout - 20000))) { console.log(log.slice(-3000)); server.kill('SIGKILL'); process.exit(1); }
  await sleep(1500);
  const sitLines = log.split('\n').filter(l => /\bSit\b|sit/i.test(l) && /(enabled|bind|Missing|Sit failed|Sit could|register|Sit )/.test(l));
  console.log('--- Sit log:\n' + sitLines.slice(0, 8).join('\n'));

  const bot = spawn('node', [botFile, '127.0.0.1', String(arg('port', '25577')), 'Bot', protocol, ids], { stdio: ['pipe', 'pipe', 'pipe'] });
  let botLog = '';
  bot.stdout.on('data', d => { botLog += d; }); bot.stderr.on('data', d => { botLog += d; });
  const joined = await waitFor(/Bot joined the game|Bot\[\S+\] logged in with entity id/, 40000);
  step('bot joined', joined, joined ? '' : botLog.slice(-600) + '\n' + log.slice(-1500));
  if (!joined) { bot.kill(); cmd('stop'); await sleep(3000); server.kill('SIGKILL'); process.exit(1); }

  await sleep(2000);
  cmd('gamerule doDaylightCycle false');
  let out = await slice('tp Bot 0 120 0', 3000);
  await sleep(3000);
  await slice('forceload add 0 0 0 0', 1500);
  await slice('fill -4 118 -4 4 118 4 stone', 1500);
  await slice('setblock 0 119 1 oak_stairs[facing=east,half=bottom]', 1500);
  await slice('setblock 2 119 1 oak_slab[type=bottom]', 1000);
  await slice('setblock 0 119 -2 oak_stairs[facing=east,half=top]', 1000);
  await slice('execute in minecraft:overworld run tp Bot 0 120 0', 1500);

  // 0. control: a vanilla lever proves the bot's right click reaches the server
  await slice('setblock 0 119 3 lever[face=floor,facing=north,powered=false]', 1000);
  bot.stdin.write('use 0 119 3 1\n'); await sleep(1500);
  out = await slice('execute if block 0 119 3 lever[powered=true]');
  step('control: bot can right click (lever)', /Test passed/.test(out), out.trim().split('\n').pop());

  // 1. top-half stair must NOT seat
  bot.stdin.write('use 0 119 -2 1\n'); await sleep(1500);
  out = await slice(`execute if entity ${SEAT}`);
  step('top stair ignored', /Test failed|No entity was found|no entities/i.test(out) || !/Test passed|count/.test(out), out.trim().split('\n').pop());

  // 2. bottom stair seats
  bot.stdin.write('use 0 119 1 1\n'); await sleep(2000);
  out = await slice(`execute if entity ${SEAT}`);
  const seated = /Test passed/.test(out);
  step('bottom stair creates seat', seated, out.trim().split('\n').pop());
  if (SEAT.includes('tag=sit_seat')) {
    // the seat must look like the plugin's: invisible (potion effect keeps the flag), saddled, facing the stair
    out = await slice('data get entity @e[tag=sit_seat,limit=1]');
    step('seat is invisible (effect)', /invisibility|Id: 14b/.test(out), '');
    step('seat has a saddle', /Saddle: 1b|saddle: \{/.test(out), '');
    step('seat faces the stair direction (east = yaw 90)', /Rotation: \[90\.0f/.test(out), (out.match(/Rotation: \[[^\]]*\]/) || [''])[0]);
    out = await slice('data get entity Bot RootVehicle.Entity.id');
    step('player rides the seat', /minecraft:pig/.test(out), out.trim().split('\n').pop());
  } else {
    // plugin seats are not persistent, so vanilla does not write them as RootVehicle: check the rider's position instead
    out = await slice('execute if entity @a[name=Bot,x=0.5,y=118.0,z=1.5,distance=..1.6]');
    step('player rides the seat (position)', /Test passed/.test(out), out.trim().split('\n').pop());
  }

  // 3. teleporting away dismounts, the seat must be removed
  await slice('execute in minecraft:overworld run tp Bot 0 120 0', 3000);
  out = await slice(`execute if entity ${SEAT}`);
  step('seat removed after dismount', /Test failed/.test(out), out.trim().split('\n').pop());

  // 4. slab seats too
  bot.stdin.write('use 2 119 1 1\n'); await sleep(2000);
  out = await slice(`execute if entity ${SEAT}`);
  step('bottom slab creates seat', /Test passed/.test(out), out.trim().split('\n').pop());

  // 5. permission gated layout ("fun": end rod -> armor stand, requires sit.fun / op)
  await slice('execute in minecraft:overworld run tp Bot 0 120 0', 3000);
  await slice('setblock 0 119 -3 end_rod[facing=up]', 1000);
  bot.stdin.write('use 0 119 -3 1\n'); await sleep(1500);
  out = await slice('execute if entity @e[type=armor_stand,x=0,y=119,z=-3,distance=..4]');
  step('permission layout denied without op', /Test failed/.test(out), out.trim().split('\n').pop());
  await slice('op Bot', 1500);
  bot.stdin.write('use 0 119 -3 1\n'); await sleep(2000);
  out = await slice('execute if entity @e[type=armor_stand,x=0,y=119,z=-3,distance=..4]');
  step('permission layout allowed for op (armor stand)', /Test passed/.test(out), out.trim().split('\n').pop());

  // 6. (mods only) log out while sitting: vanilla restores the mount on login, the mod must still clean it up
  if (SEAT.includes('tag=sit_seat')) {
    await slice('execute in minecraft:overworld run tp Bot 0 120 0', 3000);
    bot.stdin.write('use 0 119 1 1\n'); await sleep(2000);
    out = await slice(`execute if entity ${SEAT}`);
    const seatedAgain = /Test passed/.test(out);
    const leftFrom = log.length;
    try { bot.stdin.write('quit\n'); } catch (e) { /* already gone */ }
    await waitFor(/Bot left the game/, 15000, leftFrom);
    await sleep(3000);
    const bot2 = spawn('node', [botFile, '127.0.0.1', String(arg('port', '25577')), 'Bot', protocol, ids], { stdio: ['pipe', 'pipe', 'pipe'] });
    bot2.stdout.on('data', d => { botLog += d; }); bot2.stderr.on('data', d => { botLog += d; });
    const rejoined = await waitFor(/Bot joined the game|Bot\[\S+\] logged in with entity id/, 40000, log.length);
    await sleep(3000);
    out = await slice(`execute if entity ${SEAT}`);
    step('seat survives logout (vanilla restores the mount)', seatedAgain && rejoined && /Test passed/.test(out), out.trim().split('\n').pop());
    await slice('execute in minecraft:overworld run tp Bot 0 120 0', 2000);
    await sleep(6500); // restored seats get a 5 second grace period before they count as abandoned
    out = await slice(`execute if entity ${SEAT}`);
    step('restored seat removed after dismount', /Test failed/.test(out), out.trim().split('\n').pop());
    try { bot2.stdin.write('quit\n'); } catch (e) { /* already gone */ }
  }

  const allLines = log.split('\n');
  const warnings = [];
  allLines.forEach((l, i) => { if (/me\.usainsrht\.sit/.test(l) && /^(WARNING|SEVERE)/.test(allLines[i + 1] || '')) warnings.push(allLines[i + 1]); });
  warnings.splice(12);
  if (warnings.length) console.log('--- warnings:\n' + warnings.join('\n'));
  const dbg = log.split('\n').filter(l => /\[debug\]/.test(l)).slice(0, 25);
  if (dbg.length) console.log('--- debug:\n' + dbg.join('\n'));
  console.log('--- bot:\n' + botLog.split('\n').slice(-8).join('\n'));
  try { bot.stdin.write('quit\n'); } catch (e) { /* already gone */ }
  cmd('stop');
  await sleep(6000);
  server.kill('SIGKILL');
  clearTimeout(kill);
  const failed = result.steps.filter(s => !s.ok).length;
  console.log(failed ? `RESULT: ${failed} FAILED` : 'RESULT: ALL PASSED');
  process.exit(failed ? 1 : 0);
})();
