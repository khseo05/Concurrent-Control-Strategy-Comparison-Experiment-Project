# Concert Reservation System
동시성 환경에서 좌석 초과 예약을 방지하기 위한
트랜잭션 분리 기반 예약 시스템 구현 프로젝트

## 1. 프로젝트 목적
공연 티켓팅과 같은 환경에서는 여러 사용자가 동시에 예약을 시도합니다.

이 프로젝트는 다음 문제를 해결하는 것을 목표로 합니다:
- 좌석 수 초과 예약 방지
- 결제 실패 시 좌석 복구 보장
- 만료된 예약 정리
- 결제 중 만료와 확정이 동시에 발생하는 경쟁 상황 처리

## 2. 아키텍처
```
Controller
   ↓
ReservationService (비즈니스 로직 + 재시도)
   ↓
ReservationTxService (@Transactional 분리)
   ↓
Repository (JPA)
```
### 설계 핵심
- 트랜잭션 로직과 비즈니스 로직을 분리
- 결제 API는 ``` PaymentService ```로 분리
- 상태 기반 도메인 모델 적용

### 3. 예약 흐름
1. 좌석 차감 + 예약 생성 (PENDING)
2. 결제 시도 (재시도 최대 3회)
3. 성공 -> CONFIRMED
4. 실패 -> CANCELLED + 좌석 복구
5. 만료 시 -> EXPIRED + 좌석 복구

## 4. 상태 모델
```
PENDING → CONFIRMED
PENDING → CANCELLED
PENDING → EXPIRED
```
- 상태 변경은 도메인 객체 내부에서만 가능
- CANCEL / EXPIRE 시 좌석 복구 보장

## 5. 동시성 테스트
### 1. 단위 테스트
- 예약 성공 시 좌석 감소 검증
- 결제 실패 시 좌석 복구 검증
- 만료/확정 경쟁 상황 테스트

### 2. 실제 부하 테스트
```
for i in {1..100}; do curl -X POST http://localhost:8080/concerts/1/reserve & done
```
- 초과 예약 발생 없음
- 좌석 음수 상태 없음
- 결제 실패 시 복구 정상 동작

## 기술 스택
- JAVA 17
- Spring Boot
- Spring Data JPA
- H2 (In-Memory)
- JUnit5
- Mockito

## 프로젝트에서 얻은 것
- 트랜잭션 경계 설계 경험
- 동시성 경쟁 상황 테스트 설계
- 도메인 주도 상태 모델링
- 서비스 분리를 통한 책임 명확화

## 향후 확장
- 분산 환경 대응
- 로그 수집 / 관측 시스템 적용
- 실제 DB 기반 성능 비교
