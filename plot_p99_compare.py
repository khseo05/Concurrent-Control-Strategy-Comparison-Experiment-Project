import pandas as pd
import matplotlib.pyplot as plt

# CSV 읽기
df = pd.read_csv("metrics.csv")

# ns → ms 변환
df["p99_ms"] = df["p99_ms"] / 1_000_000

# 200 threads 데이터만 필터링
df_200 = df[df["threads"] == 200]

# 전략 이름과 p99 값
strategies = df_200["strategy"]
p99_values = df_200["p99_ms"]

# 그래프
plt.figure()
plt.bar(strategies, p99_values)

plt.ylabel("P99 Latency (ms)")
plt.title("P99 Comparison at 200 Threads")
plt.grid(axis="y")

plt.savefig("p99_200_comparison.png")
plt.show()
