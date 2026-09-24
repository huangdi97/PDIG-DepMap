#!/usr/bin/env node
/**
 * PDIG v0.1.0 release SBOM + third-party notices generator.
 *
 * Produces (in --out):
 *   PDIG-0.1.0-SBOM.cyclonedx.json   (CycloneDX 1.5, JSON)
 *   THIRD-PARTY-NOTICES.md           (dependency license table)
 *
 * Inputs (real build outputs, never hand-typed component lists):
 *   --desktop-libs  dir listing of the jpackage runtime classpath (jars)
 *   --android-deps  Gradle dependency tree text for previewReleaseRuntimeClasspath
 *   --out           ASCII output dir
 *
 * License assignments come from the LICENSE_MAP below keyed by Maven group;
 * each entry is a well-known SPDX id for that component.
 */
import fs from "node:fs";
import path from "node:path";

const args = process.argv.slice(2);
const get = (flag) => {
  const i = args.indexOf(flag);
  return i >= 0 ? args[i + 1] : undefined;
};
const DESKTOP_LIBS = get("--desktop-libs");
const ANDROID_DEPS = get("--android-deps");
const OUT = get("--out");
const VERSION = get("--version") || "0.1.0";

// group inference for first-party modules (project modules, not OSS deps)
const FIRST_PARTY = new Set(["app", "core", "conformance", "repos"]);

// Desktop jar filename -> { group, name } (compose / kotlin / androidx naming).
if (!DESKTOP_LIBS || !ANDROID_DEPS || !OUT) {
  console.error("usage: gen-sbom.mjs --desktop-libs <dir> --android-deps <file> --out <dir> [--version x.y.z]");
  process.exit(2);
}
const JAR_GROUP = [
  [/^kotlin-stdlib-jdk7-(.+)/, "org.jetbrains.kotlin", "kotlin-stdlib-jdk7"],
  [/^kotlin-stdlib-jdk8-(.+)/, "org.jetbrains.kotlin", "kotlin-stdlib-jdk8"],
  [/^kotlin-stdlib-([^-].*)/, "org.jetbrains.kotlin", "kotlin-stdlib"],
  [/^kotlinx-coroutines-core-jvm-(.+)/, "org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm"],
  [/^kotlinx-datetime-jvm-(.+)/, "org.jetbrains.kotlinx", "kotlinx-datetime-jvm"],
  [/^atomicfu-jvm-(.+)/, "org.jetbrains.kotlinx", "atomicfu-jvm"],
  [/^annotation-jvm-(.+)/, "androidx.annotation", "annotation-jvm"],
  [/^collection-jvm-(.+)/, "androidx.collection", "collection-jvm"],
  [/^desktop-jvm-(.+)/, "org.jetbrains.compose.desktop", "desktop-jvm"],
  [/^skiko-awt-(.+)/, "org.jetbrains.skiko", "skiko-awt"],
  [/^core-common-(.+)/, "androidx.arch.core", "core-common"],
  [/^lifecycle-(common-jvm|runtime-compose-desktop|runtime-desktop|viewmodel-desktop)-(.*)/, "androidx.lifecycle", (m) => `lifecycle-${m[1]}`],
  [/^material-icons-(core|extended)-desktop-(.+)/, "org.jetbrains.compose.material", (m) => `material-icons-${m[1]}-desktop`],
  [/^(animation|animation-core|foundation|foundation-layout|material|material-ripple|material3|runtime|runtime-saveable|ui|ui-geometry|ui-graphics|ui-text|ui-tooling-preview|ui-unit|ui-util|desktop)-desktop-(.+)/, "org.jetbrains.compose", (m) => `${m[1]}-desktop`],
  [/^skiko-awt-runtime-windows-x64-(.+)/, "org.jetbrains.skiko", "skiko-awt-runtime-windows-x64"],
  [/^bcprov-jdk18on-(.+)/, "org.bouncycastle", "bcprov-jdk18on"],
  [/^jna-platform-(.+)/, "net.java.dev.jna", "jna-platform"],
  [/^jna-(.+)/, "net.java.dev.jna", "jna"],
  [/^sqlite-jdbc-(.+)/, "org.xerial", "sqlite-jdbc"],
  [/^slf4j-api-(.+)/, "org.slf4j", "slf4j-api"],
  [/^annotations-(.+)/, "org.jetbrains", "annotations"],
  [/^slf4j-nop-(.+)/, "org.slf4j", "slf4j-nop"],
];

function parseJar(name) {
  for (const [re, group, nameFn] of JAR_GROUP) {
    const m = name.match(re);
    if (m) {
      const artifact = typeof nameFn === "function" ? nameFn(m) : nameFn;
      return { group, name: artifact, version: m[m.length - 1] };
    }
  }
  return null;
}

const LICENSE_MAP = {
  "androidx.": ["Apache-2.0"],
  "org.jetbrains": ["Apache-2.0"],
  "org.jetbrains.kotlinx": ["Apache-2.0"],
  "org.bouncycastle": ["MIT"],
  "net.java.dev.jna": ["Apache-2.0", "LGPL-2.1-only"],
  "org.xerial": ["Apache-2.0"],
  "org.slf4j": ["MIT"],
  "net.zetetic": ["BSD-3-Clause"],
  "com.google.code.gson": ["Apache-2.0"],
  "com.google.crypto.tink": ["Apache-2.0"],
  "com.google.guava": ["Apache-2.0"],
};

function licensesFor(group) {
  for (const [prefix, lic] of Object.entries(LICENSE_MAP)) {
    if (group.startsWith(prefix)) return lic;
  }
  return [];
}

const components = new Map(); // key: group:name:version

function add(group, name, version, purlBase) {
  if (!version) return;
  const key = `${group}:${name}:${version}`;
  if (components.has(key)) return;
  const purl = purlBase
    ? purlBase
    : `pkg:maven/${group}/${name}@${version}`;
  components.set(key, {
    type: "library",
    "bom-ref": `pkg:${key}`,
    group,
    name,
    version,
    purl,
    licenses: licensesFor(group).map((id) => ({ license: { id } })),
  });
}

// ---- desktop runtime classpath ----
for (const f of fs.readdirSync(DESKTOP_LIBS)) {
  if (!f.endsWith(".jar")) continue;
  const base = f.slice(0, -4);
  const firstParty = FIRST_PARTY.has(base.split("-")[0]) && /^\d/.test(base.split("-")[1] ?? "");
  if (firstParty) {
    const [name, version] = base.split("-");
    add("com.pdig", name, version);
    continue;
  }
  const parsed = parseJar(base);
  if (!parsed) {
    console.warn(`[warn] unparsed desktop jar: ${f}`);
    continue;
  }
  add(parsed.group, parsed.name, parsed.version);
}

// ---- android previewReleaseRuntimeClasspath (resolved = max version per artifact) ----
const tree = fs.readFileSync(ANDROID_DEPS, "utf8");
const coordRe = /([A-Za-z0-9._-]+):([A-Za-z0-9._-]+):([A-Za-z0-9._+-]+)/g;
const byArtifact = new Map();
for (const m of tree.matchAll(coordRe)) {
  const [, group, name, version] = m;
  if (group === "root" || version === "project" || version.startsWith("unspecified")) continue;
  const k = `${group}:${name}`;
  if (!byArtifact.has(k) || byArtifact.get(k).version < version) {
    byArtifact.set(k, { group, name, version });
  }
}
for (const { group, name, version } of byArtifact.values()) {
  add(group, name, version);
}

// ---- CycloneDX 1.5 JSON ----
const bom = {
  bomFormat: "CycloneDX",
  specVersion: "1.5",
  version: 1,
  metadata: {
    timestamp: new Date().toISOString(),
    tools: [{ vendor: "PDIG", name: "scripts/release/gen-sbom.mjs", version: VERSION }],
    component: {
      type: "application",
      "bom-ref": "pkg:maven/com.pdig/pdig@0.1.0",
      name: "PDIG (DepMap)",
      version: VERSION,
      description: "PDIG 0.1.0 Developer Preview - Windows Desktop + Android Preview",
    },
  },
  components: [...components.values()].sort((a, b) =>
    `${a.group}:${a.name}:${a.version}`.localeCompare(`${b.group}:${b.name}:${b.version}`),
  ),
  dependencies: [
    {
      ref: "pkg:maven/com.pdig/pdig@0.1.0",
      dependsOn: [...components.keys()].map((k) => `pkg:${k}`),
    },
  ],
};

const sbomFile = path.join(OUT, `PDIG-${VERSION}-SBOM.cyclonedx.json`);
fs.writeFileSync(sbomFile, JSON.stringify(bom, null, 2) + "\n");

// ---- THIRD-PARTY-NOTICES ----
const rows = [...components.values()]
  .sort((a, b) => `${a.group}:${a.name}`.localeCompare(`${b.group}:${b.name}`))
  .map(
    (c) =>
      `| ${c.group}:${c.name} | ${c.version} | ${c.licenses.length ? c.licenses.map((l) => l.license.id).join(", ") : "TBD (see upstream)"} |`,
  )
  .join("\n");

const notices = `# THIRD-PARTY-NOTICES

> PDIG 0.1.0 Developer Preview — dependency license notices.
> Generated from real build outputs (desktop jpackage runtime classpath + Android
> previewReleaseRuntimeClasspath) by \`scripts/release/gen-sbom.mjs\`.
> Project license itself is **TBD** (see \`LICENSE_DECISION.md\`); these notices
> only cover third-party components distributed in the release binaries.

## Component license table

| Component | Version | License (SPDX) |
| --- | --- | --- |
${rows}

## Notes

- androidx / org.jetbrains (Kotlin, Compose, Skiko, annotations) components are
  Apache-2.0; see each artifact's POM/LICENSE for the exact grant.
- BouncyCastle (\`bcprov-jdk18on\`) is distributed under the Bouncy Castle Licence
  (MIT-style, "The Bouncy Castle Licence").
- JNA is dual-licensed Apache-2.0 / LGPL-2.1-only; users may pick either.
- \`sqlcipher-android\` (Zetetic) is BSD-3-Clause with the OpenSSL license exception.
- Full license texts are available from the upstream projects at the URLs embedded
  in the CycloneDX SBOM (PDIG-0.1.0-SBOM.cyclonedx.json) or from
  https://spdx.org/licenses/ for each SPDX id.
`;

fs.writeFileSync(path.join(OUT, "THIRD-PARTY-NOTICES.md"), notices);
console.log(`SBOM: ${sbomFile} (${[...components.values()].length} components)`);
console.log(`NOTICES: ${path.join(OUT, "THIRD-PARTY-NOTICES.md")}`);
