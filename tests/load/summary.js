// 压测报告生成器
// 在每个测试脚本中引入：export { handleSummary } from './summary.js';

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}分${s}秒`;
}

function getMetricValue(metric, key) {
  if (!metric || !metric.values) return '-';
  const value = metric.values[key];
  return value !== undefined ? value : '-';
}

function formatNumber(value) {
  if (value === '-' || value === undefined || value === null) return '-';
  if (typeof value === 'number') {
    if (value >= 1000000) return (value / 1000000).toFixed(2) + 'M';
    if (value >= 1000) return (value / 1000).toFixed(2) + 'K';
    if (value < 0.01 && value > 0) return value.toExponential(2);
    return value.toFixed(2).replace(/\.00$/, '');
  }
  return String(value);
}

function generateMarkdown(data, metadata = {}) {
  const metrics = data.metrics || {};
  const duration = metrics.test_duration?.values?.value || 0;
  const vusMax = metrics.vus_max?.values?.value || 0;
  const httpReqs = metrics.http_reqs;
  const httpDuration = metrics.http_req_duration;
  const httpFailed = metrics.http_req_failed;
  const qps = httpReqs?.values?.rate || 0;
  const failedRate = httpFailed?.values?.rate || 0;
  const avgLatency = httpDuration?.values?.avg || 0;
  const p95Latency = httpDuration?.values?.['p(95)'] || 0;
  const p99Latency = httpDuration?.values?.['p(99)'] || 0;
  const maxLatency = httpDuration?.values?.max || 0;

  // 判断性能好坏
  let conclusion = '';
  if (p95Latency < 500 && failedRate < 0.001) {
    conclusion = '✅ 性能优秀';
  } else if (p95Latency < 1000 && failedRate < 0.01) {
    conclusion = '⚠️ 性能可接受，但有优化空间';
  } else {
    conclusion = '❌ 性能较差，需要优化';
  }

  const lines = [
    '# 压测报告',
    '',
    '## 基本信息',
    '',
    '| 项目 | 值 |',
    '|------|-----|',
    `| 测试脚本 | ${metadata.script || '-'} |`,
    `| 测试时间 | ${new Date().toLocaleString('zh-CN')} |`,
    `| 持续时间 | ${formatDuration(duration)} |`,
    `| 最大并发 (VUs) | ${vusMax} |`,
    '',
    '## 核心指标',
    '',
    '| 指标 | 值 | 说明 |',
    '|------|-----|------|',
    `| 总请求数 | ${formatNumber(httpReqs?.values?.count || 0)} | 本次压测发送的总请求数 |`,
    `| QPS | ${formatNumber(qps)} | 每秒请求数 |`,
    `| 平均响应时间 | ${formatNumber(avgLatency)} ms | 所有请求平均耗时 |`,
    `| P95 响应时间 | ${formatNumber(p95Latency)} ms | 95% 请求低于此值 |`,
    `| P99 响应时间 | ${formatNumber(p99Latency)} ms | 99% 请求低于此值 |`,
    `| 最大响应时间 | ${formatNumber(maxLatency)} ms | 最慢的一次请求 |`,
    `| 错误率 | ${(failedRate * 100).toFixed(2)}% | 失败请求占比 |`,
    '',
    '## 延迟分布',
    '',
    '| 分位 | 响应时间 |',
    '|------|----------|',
    `| min | ${formatNumber(getMetricValue(httpDuration, 'min'))} ms |`,
    `| med (P50) | ${formatNumber(getMetricValue(httpDuration, 'med'))} ms |`,
    `| p(90) | ${formatNumber(getMetricValue(httpDuration, 'p(90)'))} ms |`,
    `| p(95) | ${formatNumber(getMetricValue(httpDuration, 'p(95)'))} ms |`,
    `| p(99) | ${formatNumber(getMetricValue(httpDuration, 'p(99)'))} ms |`,
    `| max | ${formatNumber(getMetricValue(httpDuration, 'max'))} ms |`,
    '',
    '## 阈值检查',
    '',
  ];

  // 阈值检查
  let hasThresholds = false;
  for (const [name, metric] of Object.entries(metrics)) {
    if (metric.thresholds && Object.keys(metric.thresholds).length > 0) {
      hasThresholds = true;
      lines.push(`### ${name}`);
      lines.push('');
      lines.push('| 阈值 | 状态 |');
      lines.push('|------|------|');
      for (const [thresholdName, threshold] of Object.entries(metric.thresholds)) {
        const status = threshold.ok ? '✅ 通过' : '❌ 失败';
        lines.push(`| ${thresholdName} | ${status} |`);
      }
      lines.push('');
    }
  }
  if (!hasThresholds) {
    lines.push('未配置阈值。');
    lines.push('');
  }

  // HTTP 状态码分布
  const statusCodeMetrics = Object.entries(metrics).filter(([name]) =>
    name.startsWith('http_req_status_')
  );
  if (statusCodeMetrics.length > 0) {
    lines.push('## HTTP 状态码分布');
    lines.push('');
    lines.push('| 状态码 | 次数 |');
    lines.push('|--------|------|');
    statusCodeMetrics.forEach(([name, metric]) => {
      const code = name.replace('http_req_status_', '');
      lines.push(`| ${code} | ${formatNumber(metric.values.count)} |`);
    });
    lines.push('');
  }

  // 结论
  lines.push('## 结论');
  lines.push('');
  lines.push(conclusion);
  lines.push('');
  if (p95Latency >= 1000) {
    lines.push('- P95 响应时间超过 1 秒，用户体验会明显下降，建议优化后端逻辑或数据库查询。');
  }
  if (failedRate >= 0.01) {
    lines.push('- 错误率超过 1%，建议查看后端日志定位失败原因。');
  }
  lines.push('');

  return lines.join('\n');
}

function formatTimestamp(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}-${pad(date.getMinutes())}-${pad(date.getSeconds())}`;
}

export function handleSummary(data) {
  const metadata = {
    script: __ENV.K6_SCRIPT_NAME || 'k6-load-test',
  };

  const now = new Date();
  const timestamp = formatTimestamp(now);
  const markdown = generateMarkdown(data, metadata);
  const json = JSON.stringify(data, null, 2);

  return {
    // 最新报告，每次覆盖
    'report.md': markdown,
    'report.json': json,
    // 带时间戳的归档副本，方便对比多次压测结果
    [`report-${timestamp}.md`]: markdown,
    [`report-${timestamp}.json`]: json,
  };
}
