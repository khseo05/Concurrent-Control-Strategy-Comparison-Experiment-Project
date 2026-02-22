# 동시성 제어 전략 비교 실험 프로젝트
***Concert Reservation System***

상세 설계 과정 및 실험 분석은 블로그에서 확인할 수 있습니다.
- [동시성_제어_전략_비교_실험_세부내용](https://velog.io/@kang07/%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%BB%A8%ED%8A%B8%EB%A1%A4-%EC%A0%84%EB%9E%B5-%EB%B9%84%EA%B5%90-%EC%8B%A4%ED%97%98)
## 문제 정의
동시성 환경에서 좌석 예약 시스템은 오버셀링 문제가 발생할 수 있다.
단순한 락 비교가 아니라,
- 충돌 강도에 따라 어떤 전략이 더 적합한가?
를 시험으로 검증하는 것이 프로젝트의 목표이다.

## 실험 목표
- Optimistic vs Pessimistic vs State-Based 비교
- 평균이 아닌 ***P95 / P99 중심 분석***
- Retry 누적 비용을 실제 비용으로 간주
- 설계 변경이 락 전략보다 효과적인지 검증

## 실험 환경
- remainingSeats = 1000
- sleep = 100ms
- threadCount = 50 / 100 / 200
- maxRetry = 50
- 측정 지표: avg / P95 / P99 / retry / conflict

## 전략 요약
### Optimistic Lock
- @Version 기반
- 충돌 허용 후 retry
- Low contention에 유리

### Pessimistic Lock
- SELECT FOR UPDATE
- 직렬화 기반 처리
- Tail 안정적

### State-Based 설계
- PENDING -> PAID -> CANCELLED
- 좌석 감소와 결제 흐름 분리
- 충돌 구간 구조적 축소

## 실험 결과
### 평균 Latency
<img width="640" height="480" alt="avg_ms" src="https://github.com/user-attachments/assets/7cd297df-9ec7-40a1-88e8-a82da9ad4723" />

### P95 Latency
<img width="640" height="480" alt="p95_ms" src="https://github.com/user-attachments/assets/d1a81bd7-3dac-44b0-8e60-5106b1a61ff0" />

### P99 Latency
<img width="640" height="480" alt="p99_ms" src="https://github.com/user-attachments/assets/66689170-2266-4766-8565-9bde817975c4" />

### 200 Threads 기준 P99 비교
<img width="640" height="480" alt="p99_200_comparison" src="https://github.com/user-attachments/assets/39f13c12-1aaf-448a-9e4c-e4abf191759e" />

## 핵심
1. 평균 latency는 안정성을 설명하지 못한다.
2. High Contention 환경에서 Optimistic은 ***Tail Amplification*** 발생
3. P95/P99가 전략 선택의 핵심 지표
4. 설계 변경(State-Based)이 락 전략 변경보다 더 큰 효과를 보였다.

## 구조
```
ExperimentRunner
   ↓
ReservationStrategy
   ↓
ReservationTxService
   ↓
Repository (JPA)
```
관측 계층:
```
ExecutionContext (ThreadLocal)
   ↓
MetricsCollector
   ↓
P95 / P99 계산
```

## 설계 개선 과정
초기에는 단순 락 전략 비교(Phase1)에서 시작하였다.
그러나 낙관적 락은 충돌을 처리할 뿐, 충돌 구간 자체는 유지된다는 한계를 확인하였다.

이에 트랜잭션 경계를 분리하고,
예약 상태 전이(State-Based 설계)를 도입하여
충돌 구간을 구조적으로 축소하는 방식으로 개선하였다.

마지막으로 ExecutionContext 기반 관측 시스템을 구축하여
P95/P99 중심으로 전략을 정량 비교하였다.

## 향후 계획
- mock-gateway MVP(지연/실패/타임아웃 + idempotency)
- 관측 최소 세트(TPS/에러율/P95/P99 + 상태전이 로그)
- 시나리오 3~5개 + 결과표

## 결론
동시성 전략은 "어떤 락이 더 좋은가"의 문제가 아니다.
* 충돌 강도 기반으로 전략을 선택해야 한다.

- Low Contention -> Optimistic
- High Contention -> Pessimistic or State-Based
- 가능하다면 -> 충돌 구간을 구조적으로 줄이는 설계가 최적



