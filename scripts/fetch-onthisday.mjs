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
  for (let attempt = 1; attempt <= 5; attempt++) {
    try {
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
    } catch (e) {
      console.warn(`  ${mm}/${dd} ${e.message}, retry ${attempt}`);
    }
    await sleep(2000 * attempt); // 退避加长,躲开限流窗口
  }
  return { __failed: true }; // 抓不到就跳过,不中断全年;末尾汇总失败清单
}

await mkdir(OUT, { recursive: true });
let done = 0;
const failed = [];
for (let m = 1; m <= 12; m++) {
  for (let d = 1; d <= 31; d++) {
    const mm = String(m).padStart(2, '0');
    const dd = String(d).padStart(2, '0');
    const data = await fetchDay(mm, dd);
    if (data?.__failed) {
      failed.push(`${mm}/${dd}`);
    } else if (data) {
      await writeFile(`${OUT}/${mm}-${dd}.json`, JSON.stringify(data), 'utf8');
      done++;
      if (done % 30 === 0) console.log(`progress: ${done} days`);
    }
    await sleep(400);
  }
}
console.log(`done: ${done} day files written to ${OUT}`);
if (failed.length) {
  console.warn(`${failed.length} days failed (可稍后重跑补齐): ${failed.join(', ')}`);
  // 少量失败不算致命:只要抓到 300+ 天就认为可用
  if (done < 300) process.exit(1);
}
