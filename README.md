# Concert Reservation System
동시성 제어와 트랜잭션 경계를 실험한 좌석 예약 시스템

## 프로젝트 개요
이 프로젝트는 동시성 환경에서 좌석 오버셀링을 방지하는 예약 시스템을 구현하고,
락 전략별 동작 차이와 트랜잭션 경계를 직접 실험하기 위해 개발되었습니다.

단순 CRUD가 아니라,
- 동시 요청 100개 이상 환경
- 오버셀링 재현
- 락 전략 비교
- 결제 실패/재시도 흐름 분리
- 상태 기반 좌석 복구 처리
까지 포함합니다.

## Branch Strategy - 실험 기반 설계 과정
이 프로젝트는 동시성 해결 전략을 비교하기 위해 각 해결 방안을 독립적인 브랜치로 관리합니다.
| Branch                          | 설명                              |
| ------------------------------- | ------------------------------- |
| `no-lock-version`               | 동시성 제어 없음 (오버셀링 재현)             |
| `optimistic-lock`               | `@Version` 기반 낙관적 락 적용          |
| `pessimistic-lock`              | `SELECT FOR UPDATE` 기반 비관적 락 적용 |
| `main` / `feature/payment-flow` | 트랜잭션 분리 + 결제 흐름까지 포함된 최종 구조     |

브랜치 간 diff를 통해 각 전략 차이를 직접 확인 할 수 있습니다.

## Phase 1 - 동시성 제어 실험 (Lock 비교)
### 목표
의도적으로 오버셀링을 발생시키고, 락 전략별 차이를 비교

### 실험 환경
- 100 concurrnet requests
- ExecutorService + CountDownLatch
- H2 In-Memory DB

### 결과 비교
| 전략                                 | 정합성        | 특징             |
| ---------------------------------- | ---------- | -------------- |
| No Lock (@Transactional only)      | ❌ 좌석 음수 발생 | 오버셀링 재현 성공     |
| Pessimistic Lock                   | ✅ 정합성 유지   | DB 락으로 직렬화     |
| Optimistic Lock (@Version + retry) | ✅ 정합성 유지   | 충돌 발생 + 재시도 필요 |

### 관찰
- 단순 @Transactional은 동시성 문제를 해결하지 못함
- 비관적 락은 안전하지만 성능 저하 가능성 존재
- 낙관적 락은 충돌 기반 재시도 전략이 필수
#### 추가 기술적 관찰
- Default isolation level: READ_COMMITTED
- Optimisitc lock conflict is detected during flush/commit phase (JPA version check)

#### 자세한 실험 로그 및 분석은 블로그에 정리 예정

## Phase 2 - 결제 흐름 + 트랜잭션 경계 분리
동시성 제어 이후, 실제 예약 시스템 구조로 확장

### 구조 분리
#### - ReservationService
   - 결제 재시도 로직
   - 흐름 제어
#### - ReservatonTxService
   - 좌석 감소
   - 예약 상태 변경
   - 트랜잭션 경계 관리
#### - PaymentService
   - 결제 성공 / 일시 실패 / 영구 실패 시뮬레이션

## 설계 포인트
- 외부 API(결제) 호출은 트랜잭션 밖에서 수행
- 상태 기반 좌석 복구 (CANCELLED / EXPIRED)
- 재시도 초과 시 자동 취소
- 멱등성 고려 (중복 취소 방지)

## 실제 동시 요청 테스트
```
for i in {1..100}; do curl -X POST http://localhost:8080/concerts/1/reserve & done
```
#### 결과
- 좌석 수 초과 예약 없음
- 음수 좌석 발생 없음
- 일부 요청은 "좌석 부족" 응답
- 정합성 유지 확인

## 아키텍처 구조
```
Controller
   ↓
ReservationService (비즈니스 로직 + 재시도)
   ↓
ReservationTxService (@Transactional 분리)
   ↓
Repository (JPA)
```
## 실행 방법
```
./gradlew clean build
./gradlew bootRun
```
H2 Console:
```
http://localhost:8080/h2-console
```

## 기술 스택
- Language/Framework: Java 17, Spring Boot 3.x
- Persistence: Spring Data JPA, H2 (In-memory)
- Test: JUnit5, Mockito, AssertJ

## 향후 확장
- 분산 환경 대응
- 로그 수집 / 관측 시스템 적용
- 메트릭 기반 락 전략 성능 비교

## 프로젝트 의도
이 프로젝트는 다음 질문들에 대한 답을 찾는 과정이었습니다.
- "동시성은 왜 깨지는가?"
- "어떤 상황에서 어떤 락을 선택해야 하는가?"
- "외부 API 호출이 포함된 트랜잭션은 어떻게 관리해야 하는가?"

## 요약
- 오버셀링 재현
- 락 전략 비교
- 결제 재시도 구조 설계
- 상태 기반 좌석 복구
- 동시 요청 100건 검증
