import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import os

os.makedirs("charts", exist_ok=True)

COLS_15 = ["strategy", "scenario", "threads", "avg_ns", "p95_ns", "p99_ns",
           "max_ns", "avg_retry", "conflict", "tps", "error_rate",
           "dedup_hit", "dedup_hit_rate", "cb_blocked", "cb_blocked_rate"]
COLS_13 = COLS_15[:13]
COLS_11 = COLS_15[:11]

rows = []
with open("reservation-service/metrics.csv") as f:
    for line in f:
        parts = line.strip().split(",")
        if len(parts) == 15:
            rows.append(dict(zip(COLS_15, parts)))
        elif len(parts) == 13:
            d = dict(zip(COLS_13, parts))
            d["cb_blocked"] = "0"; d["cb_blocked_rate"] = "0.0"
            rows.append(d)
        elif len(parts) == 11:
            d = dict(zip(COLS_11, parts))
            d["dedup_hit"] = "0"; d["dedup_hit_rate"] = "0.0"
            d["cb_blocked"] = "0"; d["cb_blocked_rate"] = "0.0"
            rows.append(d)

df = pd.DataFrame(rows)
for col in ["threads", "tps", "avg_retry"]:
    df[col] = pd.to_numeric(df[col])
df = df.drop_duplicates(subset=["strategy", "scenario", "threads"], keep="last")

opt = df[(df["strategy"] == "optimistic") & (df["scenario"] == "success")].sort_values("threads")
pes = df[(df["strategy"] == "pessimistic") & (df["scenario"] == "success")].sort_values("threads")

threads = [50, 100, 200]
fig, ax1 = plt.subplots(figsize=(8, 5))
ax2 = ax1.twinx()

# TPS 라인
ax1.plot(opt["threads"], opt["tps"], "o-", color="#E87C4C", linewidth=2.5,
         markersize=8, label="optimistic TPS")
ax1.plot(pes["threads"], pes["tps"], "s-", color="#4C9BE8", linewidth=2.5,
         markersize=8, label="pessimistic TPS")

# avg_retry 막대 (optimistic만)
bar_width = 15
ax2.bar(opt["threads"] - bar_width / 2, opt["avg_retry"], width=bar_width,
        color="#E87C4C", alpha=0.3, label="optimistic avg_retry")

# 값 레이블
for _, row in opt.iterrows():
    ax1.text(row["threads"] + 4, row["tps"] + 5,
             f"{row['tps']:.1f}", fontsize=9, color="#E87C4C", fontweight="bold")
    ax2.text(row["threads"] - bar_width / 2, row["avg_retry"] + 0.03,
             f"{row['avg_retry']:.2f}", ha="center", fontsize=8, color="#c0603a")

for _, row in pes.iterrows():
    ax1.text(row["threads"] + 4, row["tps"] - 18,
             f"{row['tps']:.1f}", fontsize=9, color="#4C9BE8", fontweight="bold")

ax1.set_xlabel("Threads")
ax1.set_ylabel("TPS (req/s)")
ax2.set_ylabel("avg_retry (optimistic)", color="#c0603a")
ax2.tick_params(axis="y", colors="#c0603a")
ax1.set_title("Optimistic vs Pessimistic: TPS & Retry Overhead\n(success scenario)", fontsize=12)
ax1.set_xticks(threads)
ax1.set_xlim(30, 230)
ax1.set_ylim(0, max(pes["tps"].max(), opt["tps"].max()) * 1.3)
ax2.set_ylim(0, opt["avg_retry"].max() * 3.5)
ax1.grid(True, axis="y", alpha=0.3)

lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc="upper left")

ax1.annotate("retry↑ → TPS growth slows",
             xy=(100, opt[opt["threads"] == 100]["tps"].values[0]),
             xytext=(110, opt[opt["threads"] == 100]["tps"].values[0] + 60),
             fontsize=9, color="#c0603a",
             arrowprops=dict(arrowstyle="->", color="#c0603a"))

plt.tight_layout()
plt.savefig("charts/retry_vs_tps.png", dpi=150)
plt.close()
print("saved: charts/retry_vs_tps.png")
