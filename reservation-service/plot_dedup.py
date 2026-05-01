import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import os

os.makedirs("charts", exist_ok=True)

COLS_13 = ["strategy", "scenario", "threads", "avg_ns", "p95_ns", "p99_ns",
           "max_ns", "avg_retry", "conflict", "tps", "error_rate", "dedup_hit", "dedup_hit_rate"]
COLS_11 = ["strategy", "scenario", "threads", "avg_ns", "p95_ns", "p99_ns",
           "max_ns", "avg_retry", "conflict", "tps", "error_rate"]

rows = []
with open("metrics.csv") as f:
    for line in f:
        parts = line.strip().split(",")
        if len(parts) == 13:
            rows.append(dict(zip(COLS_13, parts)))
        elif len(parts) == 11:
            d = dict(zip(COLS_11, parts))
            d["dedup_hit"] = "0"
            d["dedup_hit_rate"] = "0.0"
            rows.append(d)

df = pd.DataFrame(rows)
for col in ["threads", "avg_ns", "p95_ns", "p99_ns", "tps", "error_rate", "dedup_hit", "dedup_hit_rate"]:
    df[col] = pd.to_numeric(df[col])

df["db_reached"] = df["threads"] - df["dedup_hit"]

# ── 최신 실험 데이터만 사용 (중복 제거: 마지막 등장 기준) ──────────────────
df = df.drop_duplicates(subset=["strategy", "scenario", "threads"], keep="last")

# ─────────────────────────────────────────────────────────────────────────────
# 차트 1: dedup 실험 - DB 도달 vs dedup 차단 (stacked bar)
# ─────────────────────────────────────────────────────────────────────────────
dedup_df = df[df["scenario"].str.startswith("dedup_")].copy()
scenario_order = ["dedup_unique", "dedup_mixed", "dedup_high", "dedup_burst"]
dedup_df["scenario"] = pd.Categorical(dedup_df["scenario"], categories=scenario_order, ordered=True)
dedup_df = dedup_df.sort_values("scenario")

labels = ["0% dup\n(unique)", "50% dup\n(mixed)", "75% dup\n(high)", "99.5% dup\n(burst)"]
x = np.arange(len(labels))
width = 0.5

fig, ax = plt.subplots(figsize=(8, 5))
bars1 = ax.bar(x, dedup_df["db_reached"].values, width, label="DB Reached", color="#4C9BE8")
bars2 = ax.bar(x, dedup_df["dedup_hit"].values, width, bottom=dedup_df["db_reached"].values,
               label="Redis Blocked", color="#E87C4C")

for i, (db, hit) in enumerate(zip(dedup_df["db_reached"].values, dedup_df["dedup_hit"].values)):
    if db > 0:
        ax.text(x[i], db / 2, str(int(db)), ha="center", va="center", fontsize=10, color="white", fontweight="bold")
    if hit > 0:
        ax.text(x[i], db + hit / 2, str(int(hit)), ha="center", va="center", fontsize=10, color="white", fontweight="bold")

ax.set_xticks(x)
ax.set_xticklabels(labels)
ax.set_ylabel("Request Count (threads=200)")
ax.set_title("Redis Dedup: DB Reached vs Blocked")
ax.legend()
ax.set_ylim(0, 220)
ax.yaxis.set_major_locator(ticker.MultipleLocator(50))
plt.tight_layout()
plt.savefig("charts/dedup_db_vs_blocked.png", dpi=150)
plt.close()
print("saved: charts/dedup_db_vs_blocked.png")

# ─────────────────────────────────────────────────────────────────────────────
# 차트 2: dedup 실험 - TPS 변화
# ─────────────────────────────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(8, 5))
ax.bar(x, dedup_df["tps"].values, width, color="#4C9BE8")
for i, tps in enumerate(dedup_df["tps"].values):
    ax.text(x[i], tps + 10, f"{tps:.1f}", ha="center", va="bottom", fontsize=10)

ax.set_xticks(x)
ax.set_xticklabels(labels)
ax.set_ylabel("TPS")
ax.set_title("Redis Dedup: TPS by Duplicate Rate")
ax.set_ylim(0, max(dedup_df["tps"].values) * 1.2)
plt.tight_layout()
plt.savefig("charts/dedup_tps.png", dpi=150)
plt.close()
print("saved: charts/dedup_tps.png")

# ─────────────────────────────────────────────────────────────────────────────
# 차트 3: 전략 비교 - success 시나리오, 200 threads (TPS)
# ─────────────────────────────────────────────────────────────────────────────
strategy_df = df[(df["scenario"] == "success") & (df["threads"] == 200)].copy()
strategy_order = ["optimistic", "pessimistic", "stateBased"]
strategy_df["strategy"] = pd.Categorical(strategy_df["strategy"], categories=strategy_order, ordered=True)
strategy_df = strategy_df.sort_values("strategy")

fig, ax = plt.subplots(figsize=(7, 5))
colors = ["#E87C4C", "#E8C84C", "#4C9BE8"]
bars = ax.bar(strategy_df["strategy"].values, strategy_df["tps"].values, width=0.5, color=colors)
for bar, tps in zip(bars, strategy_df["tps"].values):
    ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 5,
            f"{tps:.1f}", ha="center", va="bottom", fontsize=11)

ax.set_ylabel("TPS")
ax.set_title("Strategy TPS Comparison (success, threads=200)")
ax.set_ylim(0, max(strategy_df["tps"].values) * 1.25)
plt.tight_layout()
plt.savefig("charts/strategy_tps_200.png", dpi=150)
plt.close()
print("saved: charts/strategy_tps_200.png")

# ─────────────────────────────────────────────────────────────────────────────
# 차트 4: dedup 차단율 vs DB 도달 (선 그래프)
# ─────────────────────────────────────────────────────────────────────────────
fig, ax1 = plt.subplots(figsize=(8, 5))
ax2 = ax1.twinx()

ax1.plot(x, dedup_df["db_reached"].values, "o-", color="#4C9BE8", linewidth=2, label="DB Reached")
ax2.plot(x, dedup_df["dedup_hit_rate"].values, "s--", color="#E87C4C", linewidth=2, label="Dedup Rate (%)")

ax1.set_xticks(x)
ax1.set_xticklabels(labels)
ax1.set_ylabel("DB Reached", color="#4C9BE8")
ax2.set_ylabel("Dedup Rate (%)", color="#E87C4C")
ax1.set_title("Redis Dedup: DB Load Reduction")
ax1.set_ylim(0, 220)
ax2.set_ylim(0, 110)

lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc="center right")

plt.tight_layout()
plt.savefig("charts/dedup_db_load_reduction.png", dpi=150)
plt.close()
print("saved: charts/dedup_db_load_reduction.png")

print("\nDone. Check charts/ folder.")
