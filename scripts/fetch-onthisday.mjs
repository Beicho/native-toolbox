#!/usr/bin/env node
// 抓取维基百科中文「历史上的今天」全年数据 → server/data/onthisday/MM-DD.json
// 跑在 GitHub Actions(海外网络);国内服务器通过 api.github.com contents 中转读取。
//
// 并发抓取：原来串行跑 372 天，每天固定 sleep 400ms 再叠加最长 30s 的退避重试，
// 两次都超出 workflow 超时被 cancel（07-23、07-25）。改成固定大小工作池并发，
// 抓一天写一天，即使中途被掐也保留已完成部分。
import { mkdir, writeFile } from 'node:fs/promises';

const OUT = 'server/data/onthisday';
const UA = 'AstroKit-DataPipeline/1.0 (github.com/Beicho/native-toolbox)';
const CONCURRENCY = 8;
const MAX_ATTEMPTS = 3;
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
const failed = [];

async function worker() {
  while (cursor < jobs.length) {
    const [mm, dd] = jobs[cursor++];
    const data = await fetchDay(mm, dd);
    if (data?.__failed) {
      failed.push(`${mm}/${dd}`);
    } else if (data) {
      await writeFile(`${OUT}/${mm}-${dd}.json`, JSON.stringify(data), 'utf8');
      done++;
      if (done % 30 === 0) console.log(`  已完成 ${done} 天`);
    }
  }
}

console.log(`开始抓取，并发 ${CONCURRENCY}`);
await Promise.all(Array.from({ length: CONCURRENCY }, () => worker()));

console.log(`完成：成功 ${done} 天，失败 ${failed.length} 天`);
if (failed.length) console.log(`失败清单：${failed.join(', ')}`);
// 少量失败不算致命：只要抓到 300+ 天就认为可用
if (done < 300) {
  console.error('成功天数不足 300，视为失败');
  process.exit(1);
}
