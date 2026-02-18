import pandas as pd
import matplotlib.pyplot as plt

# CSV 읽기
df = pd.read_csv("metrics.csv")

# ns → ms 변환
df["avg_ms"] = df["avg_ms"] / 1_000_000
df["p95_ms"] = df["p95_ms"] / 1_000_000
df["p99_ms"] = df["p99_ms"] / 1_000_000

strategies = df["strategy"].unique()

metrics = ["avg_ms", "p95_ms", "p99_ms"]

for metric in metrics:
    plt.figure()

    for s in strategies:
        subset = df[df["strategy"] == s]
        plt.plot(subset["threads"], subset[metric], marker="o", label=s)

    plt.xlabel("Threads")
    plt.ylabel("Latency (ms)")
    plt.title(f"{metric} comparison")
    plt.legend()
    plt.grid(True)
    plt.savefig(f"{metric}.png")

plt.show()
