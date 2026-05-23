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
for col in ["threads", "tps", "error_rate", "dedup_hit", "dedup_hit_rate", "cb_blocked"]:
    df[col] = pd.to_numeric(df[col])
df = df.drop_duplicates(subset=["strategy", "scenario", "threads"], keep="last")

fig, axes = plt.subplots(1, 3, figsize=(16, 5))
fig.suptitle("Layer-by-Layer Defense Effect", fontsize=14, fontweight="bold", y=1.02)

# ── Panel 1: Layer 1 - DB 락 전략 TPS 비교 (success, 200 threads) ─────────────
ax1 = axes[0]
layer1 = df[(df["scenario"] == "success") & (df["threads"] == 200)].copy()
order = ["optimistic", "pessimistic", "stateBased"]
layer1["strategy"] = pd.Categorical(layer1["strategy"], categories=order, ordered=True)
layer1 = layer1.sort_values("strategy")

colors1 = ["#E87C4C", "#E8C84C", "#4C9BE8"]
bars = ax1.bar(layer1["strategy"].values, layer1["tps"].values, width=0.5, color=colors1)
for bar, tps in zip(bars, layer1["tps"].values):
    ax1.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 30,
             f"{tps:.1f}", ha="center", va="bottom", fontsize=10, fontweight="bold")

ax1.set_title("Layer 1: DB Lock Strategy\n(success, threads=200)", fontsize=11)
ax1.set_ylabel("TPS (req/s)")
ax1.set_ylim(0, max(layer1["tps"].values) * 1.25)
ax1.yaxis.set_major_locator(ticker.MultipleLocator(300))
ax1.grid(True, axis="y", alpha=0.3)
ax1.annotate("12x↑", xy=(2, layer1["tps"].values[2]),
             xytext=(1.5, layer1["tps"].values[2] * 0.7),
             fontsize=10, color="#4C9BE8", fontweight="bold")

# ── Panel 2: Layer 2 - Redis Dedup DB 부하 절감 ────────────────────────────────
ax2 = axes[1]
dedup_df = df[(df["scenario"].str.startswith("dedup_")) & (df["strategy"] == "stateBased")].copy()
scenario_order = ["dedup_unique", "dedup_mixed", "dedup_high", "dedup_burst"]
dedup_df["scenario"] = pd.Categorical(dedup_df["scenario"], categories=scenario_order, ordered=True)
dedup_df = dedup_df.sort_values("scenario")
dedup_df["db_reached"] = dedup_df["threads"] - dedup_df["dedup_hit"]

labels2 = ["0% dup\n(unique)", "50% dup\n(mixed)", "75% dup\n(high)", "99.5% dup\n(burst)"]
x2 = np.arange(len(labels2))
width2 = 0.5

b1 = ax2.bar(x2, dedup_df["db_reached"].values, width2, label="DB Reached", color="#4C9BE8")
b2 = ax2.bar(x2, dedup_df["dedup_hit"].values, width2,
             bottom=dedup_df["db_reached"].values, label="Redis Blocked", color="#E87C4C")

for i, (db, hit) in enumerate(zip(dedup_df["db_reached"].values, dedup_df["dedup_hit"].values)):
    if db > 0:
        ax2.text(x2[i], db / 2, str(int(db)), ha="center", va="center",
                 fontsize=9, color="white", fontweight="bold")
    if hit > 0:
        ax2.text(x2[i], db + hit / 2, str(int(hit)), ha="center", va="center",
                 fontsize=9, color="white", fontweight="bold")

ax2.set_title("Layer 2: Redis Dedup\nDB Load Reduction (threads=200)", fontsize=11)
ax2.set_ylabel("Request Count")
ax2.set_xticks(x2)
ax2.set_xticklabels(labels2)
ax2.set_ylim(0, 240)
ax2.yaxis.set_major_locator(ticker.MultipleLocator(50))
ax2.legend(loc="upper right")
ax2.grid(True, axis="y", alpha=0.3)

# ── Panel 3: Layer 3 - Circuit Breaker TPS + 에러율 ───────────────────────────
ax3 = axes[2]
cb_df = df[(df["scenario"].str.startswith("cb_")) & (df["strategy"] == "stateBased")].copy()
cb_order = ["cb_normal", "cb_failure", "cb_blocked"]
cb_df["scenario"] = pd.Categorical(cb_df["scenario"], categories=cb_order, ordered=True)
cb_df = cb_df.sort_values("scenario")

labels3 = ["cb_normal\n(Healthy)", "cb_failure\n(Fault→OPEN)", "cb_blocked\n(OPEN State)"]
x3 = np.arange(len(labels3))
colors3 = ["#4C9BE8", "#E87C4C", "#9B4CE8"]

bars3 = ax3.bar(x3, cb_df["tps"].values, width=0.5, color=colors3, alpha=0.85)
ax3_r = ax3.twinx()
ax3_r.plot(x3, cb_df["error_rate"].values, "D--", color="#333333",
           linewidth=2, markersize=7, label="Error Rate (%)")

for bar, tps in zip(bars3, cb_df["tps"].values):
    ax3.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 30,
             f"{tps:.0f}", ha="center", va="bottom", fontsize=10, fontweight="bold")
for xi, rate in zip(x3, cb_df["error_rate"].values):
    ax3_r.text(xi + 0.07, rate + 2, f"{rate:.0f}%", ha="left", va="bottom",
               fontsize=9, color="#333333")

ax3.set_title("Layer 3: Circuit Breaker\nTPS vs Error Rate (threads=100)", fontsize=11)
ax3.set_ylabel("TPS (req/s)", color="#4C4C4C")
ax3_r.set_ylabel("Error Rate (%)", color="#333333")
ax3.set_xticks(x3)
ax3.set_xticklabels(labels3)
ax3.set_ylim(0, max(cb_df["tps"].values) * 1.25)
ax3_r.set_ylim(0, 110)
ax3.grid(True, axis="y", alpha=0.3)

lines, lbls = ax3_r.get_legend_handles_labels()
ax3.legend(lines, lbls, loc="upper left")
ax3.annotate("18.5x↑", xy=(2, cb_df["tps"].values[2]),
             xytext=(1.4, cb_df["tps"].values[2] * 0.65),
             fontsize=10, color="#9B4CE8", fontweight="bold")

plt.tight_layout()
plt.savefig("charts/layer_effect_summary.png", dpi=150, bbox_inches="tight")
plt.close()
print("saved: charts/layer_effect_summary.png")
