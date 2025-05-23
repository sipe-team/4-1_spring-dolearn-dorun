## 🧱 1. Gatherer가 꼭 필요한 이유는?

### 🧩 기존 연산(map, filter, flatMap)의 한계

| 기존 연산자 | 제약 사항 |
| --- | --- |
| `map`, `filter` | 1:1 또는 1:0 변환만 가능, 상태 없음 |
| `flatMap` | 1:N은 가능하지만 상태 기반 논리는 어렵고 복잡 |
| `peek` | 부작용(side effect)만 허용, 제어 불가 |
| `Collector` | **종단 연산(Terminal)** 전용, 중간 연산 불가 |

### 문제 예시: 연속된 중복 제거

```java
List<String> input = List.of("a", "a", "b", "b", "b", "c");

// 원하는 결과: ["a", "b", "c"]
// filter로 구현 불가 – 이전 값 기억 못함
```

### ✅ Gatherer의 필요성

- **상태 기억** 가능 (`previous` 저장 가능)
- **입력:출력 비율이 자유로움 (1:N, N:0, N:M 가능)**
- **중간 연산으로 동작 가능**
- **조기 종료 지원 (limit, takeWhile 등)**

> 📌 정리: 기존 Stream API는 정적이고 제한적이며, 사용자 정의 중간 연산은 사실상 불가능에 가까움. Gatherer는 Java에서 처음으로 이를 가능하게 함.

---

## ⚙️ 2. 상태 기반 중간 연산의 필요성과 개선점

### 예제 1: limit

```java
Stream.of(1, 2, 3, 4, 5)
    .limit(3) // 상태(count) 필요
```

- 기존 Stream API에서는 내부적으로 구현되어 있으나, 직접 구현은 어려움
- 상태 변수 유지 불가능

### 예제 2: windowing (고정 크기 슬라이딩 윈도우)

```java
// 원하는 결과: [ [1,2], [3,4], [5] ]
Stream.of(1,2,3,4,5)
    .slidingWindow(2) // 존재하지 않음
```

🧩 Gatherer로 구현하면:

```java
stream.gather(Gatherers.windowFixed(2))
```

### Gatherer 예제: limit 구현

```java
Gatherer<Integer, Count, Integer> limit(int maxSize) {
    class Count { int remaining = maxSize; }

    return Gatherer.ofSequential(
        Count::new,
        (state, item, downstream) -> {
            if (state.remaining-- > 0) {
                return downstream.push(item);
            } else return false; // stop
        },
        (left, right) -> left, // 병렬은 무시
        (state, downstream) -> {}
    );
}
```

---

## 🧩 3. mapConcurrent와 WebFlux 비교

### 🧪 mapConcurrent란?

- **Virtual Thread** 기반 병렬 처리 Gatherer
- Stream 처리 중 특정 단계만 **병렬화 제한** 가능

```java
stream.gather(
  Gatherers.mapConcurrent(
    value -> fetchFromService(value),
    5 // 최대 동시 실행 개수
  )
);
```

### WebFlux와 비교

| 항목 | `mapConcurrent` | WebFlux (`flatMap`) |
| --- | --- | --- |
| 기반 | Virtual Thread | Publisher(Mono/Flux) |
| 병렬도 | Thread 제한으로 조절 | flatMap + concurrency 파라미터 |
| 중단 지원 | `limit`, `takeWhile`로 중단 시 하위 작업 취소 | flatMap은 명시적 취소 없음 |
| 리액티브 스트림 | ❌ | ✅ Backpressure, 구독 기반 |

### 실무 적용 포인트

- WebFlux 기반 프로젝트에서는 `mapConcurrent`보다 `flatMap().limitRate()`가 더 적합할 수도 있음
- 하지만 Virtual Thread + Stream 기반 처리만 필요한 백엔드 처리에서는 **코드가 훨씬 단순**해짐

---

## 🧬 5. Composeable Gatherer – 재사용 가능한 중간 연산 조립

### 기본 개념

- `gatherer.andThen(...)`를 통해 여러 중간 연산을 **조합 가능**
- **로직을 분리된 단위로 관리**할 수 있어 유지보수성과 테스트 용이성 향상

### 예제

```java
Gatherer<String, ?, Optional<Integer>> parseIntGatherer = ...;
Gatherer<Optional<Integer>, ?, Integer> dropEmptyGatherer = ...;

Gatherer<String, ?, Integer> combined =
  parseIntGatherer.andThen(dropEmptyGatherer);
```

> 📌 결과적으로: "12", "a", "34" → [12, 34]

### 실무 예시: 중복되는 파이프라인 추출

```java
Gatherer<String, ?, String> normalize =
  Gatherer.of(... toLowerCase, trim, removeSpecialChars ...);

stream.gather(normalize).gather(sanitizeHtml).collect(...)
```

### 장점

- 팀 내 **표준 전처리 파이프라인**으로 공통 처리 가능
- 테스트와 문서화도 용이

---

## 💬 6. 현실적인 적용 가능성 vs 복잡성

### 장점 ✅

- 복잡한 로직을 간결하게 표현
- 상태 기반 연산, 단락 평가, 병렬 처리 등 다양한 조합 가능
- 코드 추상화 수준 향상 → **의도를 더 명확하게 표현**

### 단점 ❌

- **초심자에겐 높은 진입장벽**
- 기존 stream과 다르게 **함수형 인터페이스**가 많아 디버깅 어려움
- `andThen`과 상태 조합이 잘못될 경우 **예측 어려운 결과** 발생

### 정리

| 평가 항목 | 내용 |
| --- | --- |
| 학습 난이도 | 중상 |
| 디버깅 난이도 | 높음 (특히 병렬/단락 평가 시) |
| 실무 적합성 | 상태 기반 복잡 로직에만 한정해 사용하는 것이 적절 |
| 구조화 장점 | 중복 로직의 함수화, 재사용성 ↑ |