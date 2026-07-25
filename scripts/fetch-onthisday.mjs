#!/usr/bin/env node
// 抓取维基百科中文「历史上的今天」全年数据 → server/data/onthisday/MM-DD.json
// 跑在 GitHub Actions(海外网络);国内服务器通过 api.github.com contents 中转读取。
//
// 并发抓取：原来串行跑 372 天，每天固定 sleep 400ms 再叠加最长 30s 的退避重试，
// 两次都超出 workflow 超时被 cancel（07-23、07-25）。改成固定大小工作池并发，
// 抓一天写一天，即使中途被掐也保留已完成部分。
import { mkdir, writeFile, access } from 'node:fs/promises';

const OUT = 'server/data/onthisday';
const UA = 'AstroKit-DataPipeline/1.0 (github.com/Beicho/native-toolbox)';
// 维基媒体对同一出口 IP 限流很紧：并发 8 直接全量 429（只成功 48 天）。
// 实测低并发 + 请求间隔才稳。宁可跑慢一点。
const CONCURRENCY = 2;
const MAX_ATTEMPTS = 5;
const GAP_MS = 700;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const pick = (arr, max) =>
  (arr ?? [])
    .filter((e) => e.year != null && e.text)
    .slice(0, max)
    .map((e) => ({ y: e.year, t: e.text }));

async function fetchDay(mm, dd) {
  const url = `https://api.wikimedia.org/feed/v1/wikipedia/zh/onthisday/all/${mm}/${dd}`;
  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 20000);
      const res = await fetch(url, {
        headers: { 'User-Agent': UA, Accept: 'application/json' },
        signal: controller.signal,
      });
      clearTimeout(timer);
      if (res.status === 404) return null; // 无效日期(如 02/30)
      if (res.status === 429) {
        const retryAfter = Number(res.headers.get('retry-after')) || 15;
        console.warn(`  ${mm}/${dd} 被限流，等 ${retryAfter}s`);
        await sleep(retryAfter * 1000);
        continue;
      }
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
    if (attempt < MAX_ATTEMPTS) await sleep(1500 * attempt);
  }
  return { __failed: true }; // 抓不到就跳过,不中断全年;末尾汇总失败清单
}

// 全年任务清单（31 天月份的多余日期靠 404 过滤）
const jobs = [];
for (let m = 1; m <= 12; m++) {
  for (let d = 1; d <= 31; d++) {
    jobs.push([String(m).padStart(2, '0'), String(d).padStart(2, '0')]);
  }
}

await mkdir(OUT, { recursive: true });

let cursor = 0;
let done = 0;
let skipped = 0;
const failed = [];

async function alreadyHave(mm, dd) {
  try {
    await access(`${OUT}/${mm}-${dd}.json`);
    return true;
  } catch {
    return false;
  }
}

async function worker() {
  while (cursor < jobs.length) {
    const [mm, dd] = jobs[cursor++];
    // 增量补齐：已抓到的天数直接跳过，多跑几次就能凑满全年
    if (await alreadyHave(mm, dd)) {
      skipped++;
      continue;
    }
    const data = await fetchDay(mm, dd);
    if (data?.__failed) {
      failed.push(`${mm}/${dd}`);
    } else if (data) {
      await writeFile(`${OUT}/${mm}-${dd}.json`, JSON.stringify(data), 'utf8');
      done++;
      if (done % 30 === 0) console.log(`  本次已抓 ${done} 天`);
    }
    await sleep(GAP_MS);
  }
}

console.log(`开始抓取，并发 ${CONCURRENCY}`);
await Promise.all(Array.from({ length: CONCURRENCY }, () => worker()));

const total = done + skipped;
console.log(`本次新抓 ${done} 天，跳过已有 ${skipped} 天，合计 ${total} 天，失败 ${failed.length} 天`);
if (failed.length) console.log(`失败清单：${failed.join(', ')}`);
// 不再因为没凑满就退出失败：抓到的部分照样提交，重跑即可增量补齐
if (total < 300) {
  console.warn(`只有 ${total} 天，还没凑满全年，再跑一次这个 workflow 可以继续补。`);
}
