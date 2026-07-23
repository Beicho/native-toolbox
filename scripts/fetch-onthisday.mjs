#!/usr/bin/env node
// 抓取维基百科中文「历史上的今天」全年数据 → server/data/onthisday/MM-DD.json
// 跑在 GitHub Actions(海外网络);国内服务器通过 api.github.com contents 中转读取。
import { mkdir, writeFile } from 'node:fs/promises';

const OUT = 'server/data/onthisday';
const UA = 'AstroKit-DataPipeline/1.0 (github.com/Beicho/native-toolbox)';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const pick = (arr, max) =>
  (arr ?? [])
    .filter((e) => e.year != null && e.text)
    .slice(0, max)
    .map((e) => ({ y: e.year, t: e.text }));

async function fetchDay(mm, dd) {
  const url = `https://api.wikimedia.org/feed/v1/wikipedia/zh/onthisday/all/${mm}/${dd}`;
  for (let attempt = 1; attempt <= 3; attempt++) {
    const res = await fetch(url, { headers: { 'User-Agent': UA, Accept: 'application/json' } });
    if (res.status === 404) return null; // 无效日期(如 02/30)
    if (res.ok) {
      const j = await res.json();
      return {
        selected: pick(j.selected, 12),
        events: pick(j.events, 30),
        births: pick(j.births, 15),
        deaths: pick(j.deaths, 15),
      };
    }
    console.warn(`  ${mm}/${dd} HTTP ${res.status}, retry ${attempt}`);
    await sleep(1500 * attempt);
  }
  throw new Error(`fetch failed: ${mm}/${dd}`);
}

await mkdir(OUT, { recursive: true });
let done = 0;
for (let m = 1; m <= 12; m++) {
  for (let d = 1; d <= 31; d++) {
    const mm = String(m).padStart(2, '0');
    const dd = String(d).padStart(2, '0');
    const data = await fetchDay(mm, dd);
    if (data) {
      await writeFile(`${OUT}/${mm}-${dd}.json`, JSON.stringify(data), 'utf8');
      done++;
      if (done % 30 === 0) console.log(`progress: ${done} days`);
    }
    await sleep(250);
  }
}
console.log(`done: ${done} day files written to ${OUT}`);
