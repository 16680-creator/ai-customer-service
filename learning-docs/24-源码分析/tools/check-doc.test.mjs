import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { checkMarkdown, runCheck } from './check-doc.mjs';

const source = new Map([
  ['Example.java', new Map(['first', 'second', 'third', 'fourth', 'fifth', 'sixth', 'tail'].map((line, i) => [i + 1, line]))],
]);
const block = (start, end, body, fence = '```', lang = 'java') =>
  [`${fence}${lang} (src: Example.java#L${start}-L${end})`, ...body, fence].join('\n');
const check = (text, sources = source) => checkMarkdown(text, {
  file: resolve('chapter.md'), root: resolve('.'), sources, linkExists: () => false,
});

async function workspace(t) {
  const root = await mkdtemp(join(tmpdir(), 'check-doc-test-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  return root;
}

test('checks every scope and deduplicates overlapping files and directories', async (t) => {
  const root = await workspace(t);
  await mkdir(join(root, 'a', 'nested'), { recursive: true });
  await mkdir(join(root, 'b'));
  await writeFile(join(root, 'a', 'README.md'), '# A\n');
  await writeFile(join(root, 'a', 'nested', 'detail.md'), '## 小结\n## 自测题\n');
  await writeFile(join(root, 'b', 'README.md'), '[broken](missing.md)\n');
  const { report, exitCode } = await runCheck([
    'a', '--json', 'b', 'a', 'a/README.md', join(root, 'a', 'nested', 'detail.md'),
  ], { root });
  assert.deepEqual(Object.keys(report), ['files', 'errors', 'warns', 'results']);
  assert.equal(report.files, 3);
  assert.deepEqual(report.results.map((r) => r.file).sort(), ['a/README.md', 'a/nested/detail.md', 'b/README.md']);
  assert.equal(report.errors, 1);
  assert.equal(exitCode, 1);
  assert.match(report.results.find((r) => r.file === 'b/README.md').errors[0], /missing\.md/);
});

test('rejects nonexistent scopes, unknown arguments and non-Markdown files', async (t) => {
  const root = await workspace(t);
  await writeFile(join(root, 'README.md'), '# OK\n');
  await writeFile(join(root, 'plain.txt'), 'not markdown');
  for (const args of [['missing'], ['README.md', 'missing'], ['--unknown'], ['plain.txt'], ['']]) {
    const { report, exitCode } = await runCheck([...args, '--json'], { root });
    assert.equal(exitCode, 2, JSON.stringify(args));
    assert.ok(report.errors > 0, JSON.stringify(args));
    assert.equal(report.files, 0, 'invalid scopes must not silently produce a partial pass');
  }
});

test('rejects reversed, zero, unsafe and out-of-source ranges, including omitted gaps', () => {
  const cases = [
    block(4, 2, ['fourth']), block(0, 1, ['first']), block(1, 8, ['first', '...', 'tail']),
    block(9007199254740992, 9007199254740992, ['first']),
    block(1, 7, ['first', '...', 'tail']),
    '```java (src: Example.java#L-1-L2)\nfirst\n```',
  ];
  const gapSource = new Map([['Example.java', new Map([[1, 'first'], [7, 'tail']])]]);
  for (const [i, text] of cases.entries()) {
    assert.ok(check(text, i === 4 ? gapSource : source).errors.length > 0, text);
  }
  assert.deepEqual(check(block(1, 3, ['first', 'second', 'third'])).errors, []);
});

test('requires the exact first line and both endpoints without omissions', () => {
  for (const text of [
    block(1, 3, ['second', 'third']),
    block(1, 3, ['first', 'second']),
    block(1, 2, ['first', 'second', 'third']),
    block(1, 4, ['first', 'third', 'fourth']),
    block(1, 7, ['...', 'second', '...', 'tail']),
    block(1, 1, []),
  ]) assert.ok(check(text).errors.length > 0, text);
});

test('bounds omitted continuations and requires an actual tail at the declared end', () => {
  for (const marker of ['...', '// ...', '…', '// ⋯', '/* ... */']) {
    assert.deepEqual(check(block(1, 7, ['first', marker, 'sixth', 'tail'])).errors, [], marker);
  }
  for (const text of [
    block(1, 6, ['first', '...', 'tail']),
    block(1, 7, ['first', '...', 'sixth']),
    block(1, 7, ['first', '...']),
    block(1, 7, ['first', '// ... fabricated code', 'tail']),
    block(1, 7, ['first', '...fabricated', 'tail']),
    block(1, 7, ['first', 'third', '...', 'tail']),
    block(1, 7, ['first', '...', 'sixth', 'fourth', 'tail']),
  ]) assert.ok(check(text).errors.length > 0, text);
});

test('matches complete omitted segments and handles repeated source lines at the tail', () => {
  const repeated = new Map([['Example.java', new Map([
    [1, 'first'], [2, 'hidden'], [3, '}'], [4, 'middle'], [5, '}'],
  ])]]);
  assert.deepEqual(check(block(1, 5, ['first', '...', '}']), repeated).errors, []);
  const longLines = ['first', 'hidden', ...Array.from({ length: 21 }, (_, i) => `line-${i}`), 'tail'];
  const longSource = new Map([['Example.java', new Map(longLines.map((line, i) => [i + 1, line]))]]);
  const body = ['first', '...', ...longLines.slice(2)];
  body[22] = 'fabricated';
  assert.ok(check(block(1, 24, body), longSource).errors.length > 0);
});

test('parses fence scope once and ignores links and source examples inside real code', () => {
  const demonstration = [
    '````markdown', '```java (src: Missing.java#L98-L89)', 'invented', '```',
    '[example](missing.md)', '[[源码:Missing.java#L1-L2]]', '````',
    '```text', '[example](missing-too.md)', '```',
  ].join('\n');
  assert.deepEqual(check(demonstration).errors, []);
  assert.equal(check(`${demonstration}\n[real](outside.md)`).errors.length, 1);
  assert.ok(check(`${demonstration}\n${block(4, 2, ['fourth'])}`).errors.length > 0);
  assert.deepEqual(check(block(1, 3, ['first', 'second', 'third'], '~~~~')).errors, []);
  assert.ok(check(block(4, 2, ['fourth'], '~~~~', 'text')).errors.length > 0);
  assert.ok(check('```java (src: Example.java#L1-L1)\nfirst').errors.length > 0);
});
