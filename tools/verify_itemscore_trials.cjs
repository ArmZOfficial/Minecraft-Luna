/** Trial content validation uses the actual official helper, not an imitation of its item schema. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '..');
const content = path.join(root, 'server/content/itemscore');
const pack = JSON.parse(fs.readFileSync(path.join(content, 'manifest.json'), 'utf8'));
const helper = path.resolve(process.argv[2] || path.join(root, `.tools/itemscore-helper-${pack.helper.version}`));
const pkg = JSON.parse(fs.readFileSync(path.join(helper, 'package.json'), 'utf8'));
assert.equal(pkg.name, pack.helper.package);
assert.equal(pkg.version, pack.helper.version, 'Use the pinned helper version for reproducible validation');
const M = require(path.join(helper, 'lib/manifest.js'));
const idx = M.buildIndex(M.loadManifest(process.argv[3] || path.join(helper, 'data/itemscore-api.json')));
const report = { helper: pkg.version, api: idx.manifest.pluginVersion, source: idx.source, runtime_tested: false, items: [] };

function checkCalls(value) {
  if (Array.isArray(value)) { value.forEach(checkCalls); return; }
  if (value === null || typeof value !== 'object') { return; }
  assert.equal('expr' in value, false, 'Opaque expressions are not part of these GUI-editable trials');
  if (value.call) {
    assert.ok(!/console|opPlayer|giveItem|giveCustomItem|removeHeldItem|explosion|breakBlock|veinMine|teleport|economy/i.test(value.call),
      `Unexpected privileged/world/inventory call in trial: ${value.call}`);
    if (value.call === 'core.executeCommand') {
      assert.deepEqual(value.args, [{ var: 'player' }, 'menu'], 'Only /menu as the player is allowed');
    }
  }
  Object.values(value).forEach(checkCalls);
}

const ids = new Set();
for (const entry of pack.items) {
  assert.ok(!ids.has(entry.id), 'Duplicate trial ID');
  ids.add(entry.id);
  const file = path.resolve(content, entry.file);
  assert.ok(file.startsWith(content + path.sep), 'Trial path must stay inside content package');
  const item = JSON.parse(fs.readFileSync(file, 'utf8'));
  assert.equal(item.name, entry.id);
  assert.equal(item.material, entry.material);
  assert.equal(item.stackable, false);
  assert.deepEqual(item.requirements, [{ type: 'permission', input: pack.trial_permission }]);
  assert.ok(!item.recipe && !item.loot && !item.stats && !item.skin && !item.texture, 'Trial must not enable unverified recipes, loot, stats or model providers');
  assert.deepEqual(item.customEvents, []);
  checkCalls(item);
  const result = M.validateItem(idx, item);
  report.items.push({ id: item.name, ...result });
  assert.equal(result.valid, true, `${item.name}: ${result.errors.join('; ')}`);
}
const importFiles = fs.readdirSync(path.join(content, 'imports')).filter(f => f.endsWith('.import')).sort();
assert.deepEqual(importFiles, pack.items.map(e => path.basename(e.file)).sort(), 'Every import must be declared and validated');
console.log(JSON.stringify(report, null, 2));
