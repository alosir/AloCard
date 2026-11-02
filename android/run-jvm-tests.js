/**
 * Android 工程的 JVM 侧构建与测试脚本。
 *
 * 本工程没有 Android SDK / Gradle，无法编译 APK。但领域层（DateEngine 等）
 * 不依赖 Android API，可以在 JVM 上编译并测试。
 * 本脚本负责这部分，让核心逻辑有真实的验证。
 *
 * 用法：
 *   node android/run-jvm-tests.js compile
 *   node android/run-jvm-tests.js test
 */
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname);
const LIB = 'C:/Users/白泽/.workbuddy/binaries/kotlin/lib';
const JAVA = 'C:/Program Files/Java/jdk-26/bin/java.exe';

const COMPILER_CP = [
  `${LIB}/kotlin-compiler-2.2.0.jar`,
  `${LIB}/kotlin-stdlib-2.2.0.jar`,
  `${LIB}/kotlin-reflect-2.2.0.jar`,
  `${LIB}/kotlinx-coroutines-core-jvm-1.9.0.jar`,
  `${LIB}/kotlin-script-runtime-2.2.0.jar`,
  `${LIB}/annotations-13.0.jar`,
].join(';');

const DEPS_CP = [
  `${LIB}/kotlin-stdlib-2.2.0.jar`,
  `${LIB}/kotlinx-coroutines-core-jvm-1.9.0.jar`,
  `${LIB}/junit-4.13.2.jar`,
  `${LIB}/hamcrest-core-1.3.jar`,
].join(';');

const MAIN_SRC = path.join(ROOT, 'app/src/main/kotlin');
const TEST_SRC = path.join(ROOT, 'app/src/test/kotlin');
const OUT = path.join(ROOT, 'build/classes');
const TEST_OUT = path.join(ROOT, 'build/test-classes');

function kotlinc(args, label) {
  const r = spawnSync(JAVA, ['-cp', COMPILER_CP, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
  const out = (r.stdout || '') + (r.stderr || '');
  const errors = out
    .split('\n')
    .filter((l) => /error:|exception:/.test(l))
    .slice(0, 12);
  if (errors.length) {
    console.log(`[${label}] 编译失败：`);
    errors.forEach((e) => console.log('  ' + e.trim()));
    return false;
  }
  console.log(`[${label}] 编译通过`);
  return true;
}

function run(cmd, label) {
  const r = spawnSync(JAVA, ['-cp', cmd.join(';'), ...([])], { cwd: ROOT, encoding: 'utf8' });
  return r;
}

// ---------- 收集 Kotlin 源文件 ----------
function collect(dir) {
  if (!fs.existsSync(dir)) return [];
  const out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...collect(p));
    else if (e.name.endsWith('.kt')) out.push(p);
  }
  return out;
}

const mode = process.argv[2] || 'test';

fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(TEST_OUT, { recursive: true });

const mainFiles = collect(MAIN_SRC);
const testFiles = collect(TEST_SRC);
console.log(`主源码 ${mainFiles.length} 个，测试源码 ${testFiles.length} 个\n`);

// 只编译不依赖 Android API 的文件：domain 包下的全部。
// ui / data / work / widget 依赖 Android SDK，本机没有，跳过。
const jvmMain = mainFiles.filter((f) => f.includes(`${path.sep}domain${path.sep}`));
const jvmTest = testFiles.filter((f) => f.includes(`${path.sep}domain${path.sep}`));

console.log(`可在 JVM 编译的主源码：${jvmMain.length} 个`);
console.log(`可在 JVM 编译的测试：${jvmTest.length} 个\n`);

if (!kotlinc(['-no-stdlib', '-no-reflect', '-nowarn', '-classpath', DEPS_CP, '-d', OUT, ...jvmMain], '主源码')) {
  process.exit(1);
}

if (jvmTest.length > 0) {
  if (!kotlinc(
    ['-no-stdlib', '-no-reflect', '-nowarn', '-classpath', `${DEPS_CP};${OUT}`, '-d', TEST_OUT, ...jvmTest],
    '测试'
  )) {
    process.exit(1);
  }
}

// ---------- 运行 JUnit ----------
const testClasses = fs.readdirSync(TEST_OUT, { withFileTypes: true, recursive: true })
  .filter((e) => e.isFile && e.name.endsWith('Test.class') && !e.name.includes('$'))
  .map((e) => {
    const rel = path.relative(TEST_OUT, path.join(e.parentPath || e.path, e.name));
    return rel.replace(/\.class$/, '').split(path.sep).join('.');
  });

console.log(`\n待运行测试类：${testClasses.join(', ')}\n`);

const runCp = [OUT, TEST_OUT, ...DEPS_CP.split(';')].join(';');
for (const cls of testClasses) {
  const r = spawnSync(JAVA, ['-cp', runCp, 'org.junit.runner.JUnitCore', cls], { encoding: 'utf8' });
  const out = (r.stdout || '') + (r.stderr || '');
  console.log(out.trim());
  if (r.status !== 0) {
    console.log(`\n测试类 ${cls} 运行失败（exit ${r.status}）`);
    process.exit(1);
  }
}

console.log('\n全部通过。');
