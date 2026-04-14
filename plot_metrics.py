import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import os

df = pd.read_csv("metrics.csv")

# ns → ms 변환
for col in ["avg_ns", "p95_ns", "p99_ns", "max_ns"]:
    df[col.replace("_ns", "_ms")] = df[col] / 1_000_000

STRATEGIES   = ["optimistic", "pessimistic", "stateBased"]
SCENARIOS    = ["success", "fail", "timeout", "success+idempotency"]
THREAD_COUNTS = [50, 100, 200]
COLORS       = {"optimistic": "#e74c3c", "pessimistic": "#3498db", "stateBased": "#2ecc71"}
MARKERS      = {"optimistic": "o", "pessimistic": "s", "stateBased": "^"}

os.makedirs("charts", exist_ok=True)

# ── 1. 시나리오별 P99 latency ──────────────────────────────────────────────
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
fig.suptitle("P99 Write Latency by Scenario (ms)", fontsize=15, fontweight="bold")

for idx, scenario in enumerate(SCENARIOS):
    ax = axes[idx // 2][idx % 2]
    sub = df[df["scenario"] == scenario]

    for s in STRATEGIES:
        d = sub[sub["strategy"] == s].sort_values("threads")
        ax.plot(d["threads"], d["p99_ms"],
                marker=MARKERS[s], color=COLORS[s], label=s, linewidth=2)

    ax.set_title(f"[{scenario}]")
    ax.set_xlabel("Threads")
    ax.set_ylabel("P99 (ms)")
    ax.set_xticks(THREAD_COUNTS)
    ax.legend()
    ax.grid(True, alpha=0.4)

plt.tight_layout()
plt.savefig("charts/p99_by_scenario.png", dpi=150)
plt.close()

# ── 2. 시나리오별 P95 latency ──────────────────────────────────────────────
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
fig.suptitle("P95 Write Latency by Scenario (ms)", fontsize=15, fontweight="bold")

for idx, scenario in enumerate(SCENARIOS):
    ax = axes[idx // 2][idx % 2]
    sub = df[df["scenario"] == scenario]

    for s in STRATEGIES:
        d = sub[sub["strategy"] == s].sort_values("threads")
        ax.plot(d["threads"], d["p95_ms"],
                marker=MARKERS[s], color=COLORS[s], label=s, linewidth=2)

    ax.set_title(f"[{scenario}]")
    ax.set_xlabel("Threads")
    ax.set_ylabel("P95 (ms)")
    ax.set_xticks(THREAD_COUNTS)
    ax.legend()
    ax.grid(True, alpha=0.4)

plt.tight_layout()
plt.savefig("charts/p95_by_scenario.png", dpi=150)
plt.close()

# ── 3. TPS 비교 (success 시나리오) ────────────────────────────────────────
fig, ax = plt.subplots(figsize=(9, 6))
sub = df[df["scenario"] == "success"]

for s in STRATEGIES:
    d = sub[sub["strategy"] == s].sort_values("threads")
    ax.plot(d["threads"], d["tps"],
            marker=MARKERS[s], color=COLORS[s], label=s, linewidth=2)

ax.set_title("TPS Comparison — success scenario", fontsize=13, fontweight="bold")
ax.set_xlabel("Threads")
ax.set_ylabel("TPS (req/s)")
ax.set_xticks(THREAD_COUNTS)
ax.legend()
ax.grid(True, alpha=0.4)
plt.tight_layout()
plt.savefig("charts/tps_success.png", dpi=150)
plt.close()

# ── 4. 에러율 비교 (시나리오별 200 threads) ───────────────────────────────
fig, ax = plt.subplots(figsize=(10, 6))
sub = df[df["threads"] == 200]

x = range(len(SCENARIOS))
width = 0.25

for i, s in enumerate(STRATEGIES):
    d = sub[sub["strategy"] == s].set_index("scenario")
    vals = [d.loc[sc, "error_rate"] if sc in d.index else 0 for sc in SCENARIOS]
    offset = (i - 1) * width
    ax.bar([xi + offset for xi in x], vals, width, label=s, color=COLORS[s], alpha=0.85)

ax.set_title("Error Rate by Scenario — 200 threads (%)", fontsize=13, fontweight="bold")
ax.set_ylabel("Error Rate (%)")
ax.set_xticks(list(x))
ax.set_xticklabels(SCENARIOS)
ax.legend()
ax.grid(True, axis="y", alpha=0.4)
plt.tight_layout()
plt.savefig("charts/error_rate_200threads.png", dpi=150)
plt.close()

# ── 5. 충돌 횟수 비교 (success 시나리오) ─────────────────────────────────
fig, ax = plt.subplots(figsize=(9, 6))
sub = df[df["scenario"] == "success"]

for s in STRATEGIES:
    d = sub[sub["strategy"] == s].sort_values("threads")
    ax.plot(d["threads"], d["conflict"],
            marker=MARKERS[s], color=COLORS[s], label=s, linewidth=2)

ax.set_title("Conflict Count — success scenario", fontsize=13, fontweight="bold")
ax.set_xlabel("Threads")
ax.set_ylabel("Total Conflicts")
ax.set_xticks(THREAD_COUNTS)
ax.legend()
ax.grid(True, alpha=0.4)
plt.tight_layout()
plt.savefig("charts/conflict_success.png", dpi=150)
plt.close()

# ── 6. TPS 비교 — 시나리오별 (200 threads) ───────────────────────────────
fig, ax = plt.subplots(figsize=(10, 6))
sub = df[df["threads"] == 200]

x = range(len(SCENARIOS))
width = 0.25

for i, s in enumerate(STRATEGIES):
    d = sub[sub["strategy"] == s].set_index("scenario")
    vals = [d.loc[sc, "tps"] if sc in d.index else 0 for sc in SCENARIOS]
    offset = (i - 1) * width
    ax.bar([xi + offset for xi in x], vals, width, label=s, color=COLORS[s], alpha=0.85)

ax.set_title("TPS by Scenario — 200 threads", fontsize=13, fontweight="bold")
ax.set_ylabel("TPS (req/s)")
ax.set_xticks(list(x))
ax.set_xticklabels(SCENARIOS)
ax.legend()
ax.grid(True, axis="y", alpha=0.4)
plt.tight_layout()
plt.savefig("charts/tps_by_scenario.png", dpi=150)
plt.close()

# ── 7. success vs timeout TPS 직접 비교 ──────────────────────────────────
fig, axes = plt.subplots(1, 3, figsize=(15, 5), sharey=False)
fig.suptitle("TPS Drop: success vs timeout (Gateway Fault Impact)", fontsize=13, fontweight="bold")

for idx, s in enumerate(STRATEGIES):
    ax = axes[idx]
    for scenario, linestyle in [("success", "-"), ("timeout", "--")]:
        d = df[(df["strategy"] == s) & (df["scenario"] == scenario)].sort_values("threads")
        ax.plot(d["threads"], d["tps"],
                marker=MARKERS[s], color=COLORS[s],
                linestyle=linestyle, linewidth=2,
                label=scenario)
    ax.set_title(s, fontweight="bold")
    ax.set_xlabel("Threads")
    ax.set_ylabel("TPS (req/s)")
    ax.set_xticks(THREAD_COUNTS)
    ax.legend()
    ax.grid(True, alpha=0.4)

plt.tight_layout()
plt.savefig("charts/tps_success_vs_timeout.png", dpi=150)
plt.close()

# ── 8. 요약 테이블 출력 ───────────────────────────────────────────────────
print("\n" + "=" * 70)
print("SUMMARY TABLE — success scenario, P99 (ms) / TPS")
print("=" * 70)
sub = df[df["scenario"] == "success"]
print(f"{'Strategy':<14} {'Threads':<10} {'P99 (ms)':>10} {'TPS':>10} {'Conflict':>10}")
print("-" * 70)
for s in STRATEGIES:
    for t in THREAD_COUNTS:
        row = sub[(sub["strategy"] == s) & (sub["threads"] == t)]
        if not row.empty:
            p99 = row["p99_ms"].values[0]
            tps = row["tps"].values[0]
            conf = int(row["conflict"].values[0])
            print(f"{s:<14} {t:<10} {p99:>10.1f} {tps:>10.1f} {conf:>10}")
print("=" * 70)

print("\nCharts saved to ./charts/")
print("  p99_by_scenario.png")
print("  p95_by_scenario.png")
print("  tps_success.png")
print("  error_rate_200threads.png")
print("  conflict_success.png")
print("  tps_by_scenario.png")
print("  tps_success_vs_timeout.png")
