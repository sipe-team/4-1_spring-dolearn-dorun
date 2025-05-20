## ✅ 1. 🔄 JDK 17 이후 성능 개선 흐름 요약

> ❓ 왜 JDK 21, 22, 23이 더 빠를까?
>

### 🔍 핵심 배경

Java 17은 LTS이긴 하나 **성능 면에서 JDK 21~23의 변화는 매우 크며**, 그 이유는 다음과 같은 **JVM 내부 개선** 때문입니다.

### 🛠 주요 내부 최적화 요소

| 범주 | 변화 및 효과                                                                                                          |
| --- |------------------------------------------------------------------------------------------------------------------|
| **C2 컴파일러** | - Autovectorization 향상 <br/>- Loop Unrolling, Inlining 등 aggressive optimization <br/> - `Merged Store` 패턴 자동 감지 |
| **GC 개선** | - ZGC의 Generational GC 기본 적용 (JDK 24) <br/> - Compact Object Header 실험 기능 도입                                     |
| **Foreign Function & Memory API (Panama)** | - Native call 대체 가능- 메모리 세그먼트 최적화로 native 대비 더 빠른 처리                                                             |
| **String Concatenation 최적화** | - MethodHandle 조합 → Hidden Class로 변경- **시작 속도 40% 향상**                                                           |

### 📈 체감 성능 변화

- **스타트업 시간** 개선: Leiden, CDS 최적화, string concat 변경
- **처리량(throughput)** 증가: 벡터 연산, GC latency 감소
- **메모리 효율** 향상: Compact headers, Valhalla 준비
- **신기능 도입**: Virtual Thread, Structured Concurrency → 동시성 제어 간편화

### ⚖️ 17 → 21 이상 업그레이드 고려 시 장단점

| 항목 | 고려 요소                                                                                                         |
| --- |---------------------------------------------------------------------------------------------------------------|
| ✅ **장점** | - 성능 및 GC 향상 <br/>- Loom ( virtual thread), Record 등 도입 <br/> - API 최적화 및 startup 개선                          |
| ⚠️ **주의점** | - 일부 API deprecated or 제거 <br/> - `preview` 기능에 대한 JVM 옵션 필요 <br/> - 프레임워크 호환성 (e.g., Spring Boot, JPA 버전 확인) |

---

## ✅ 2. 🧵 Loom (가상 스레드)의 실무 적용 가능성

> ❓ Webflux는 이미 논블로킹인데, Loom은 왜 필요할까?

### 🧠 기본 개념 정리

| 항목 | 설명 |
| --- | --- |
| **가상 스레드 (Virtual Thread)** | OS 스레드와 1:1 매핑하지 않고, JVM이 스케줄링하는 경량 스레드 |
| **Thread.start() ≠ 새로운 OS thread** | → context switching 비용 없음→ 수십만 개의 동시 스레드 처리 가능 |

### 🧵 Loom vs WebFlux 구조 비교

| 항목 | WebFlux | Loom 기반 MVC |
| --- | --- | --- |
| **프로그래밍 모델** | Reactive (Flux/Mono) | Imperative (동기 코드 유지) |
| **동시성 처리** | 이벤트 루프 기반, 비동기 Non-Blocking | Virtual Thread를 통한 동기 흐름 |
| **학습 곡선** | 높음 (Reactor 연산자 학습 필요) | 낮음 (기존 코드 재사용 가능) |
| **디버깅/트레이싱** | 복잡함 | 단순화됨 |
| **성능** | 고성능이나 상황에 따라 역효과 가능 | 대량 스레드 필요 시 유리 |

### ✅ 실무 적용 포인트

- **DB/외부 API 호출이 많은 비즈니스 로직**은 Loom이 더 직관적
- Reactive에 익숙하지 않은 팀이라면 **코드 가독성 및 유지보수 비용↓**
- **Tomcat/Netty → Virtual Thread 기반 Executor 전환** 가능성

```java
// 실습 : Loom vs Thread 비교 - 간단한 백그라운드 작업 실행

public class VirtualThreadExample {
    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            try {
                Thread.sleep(100);
                System.out.println("[" + Thread.currentThread() + "] Finished");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        System.out.println("▶ Traditional Threads");
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(task);
            thread.start();
        }

        Thread.sleep(200);
        System.out.println("▶ Virtual Threads");
        for (int i = 0; i < 5; i++) {
            Thread.startVirtualThread(task);
        }

        Thread.sleep(200);
    }
}
```

---

## ✅ 3. 💡 Valhalla 프로젝트: Value Object의 성능적 이점

> ❓ 왜 값 객체(Value Object)가 성능에 영향을 줄까?
>

### 🧬 배경 개념: Value Object / Inline Class

기존 Java 객체는 **참조 기반(reference)**으로 동작하여, 배열이나 컬렉션에 객체를 넣으면 **간접 메모리 접근(2차 포인터)**이 필요합니다.

Valhalla는 객체를 **값 기반(flat layout)**으로 저장하여 캐시 지역성(locality)과 메모리 효율을 개선합니다.

### 📊 성능적 이점

| 구조 | 기존 JVM | Valhalla 적용 시 |
| --- | --- | --- |
| `Point[]` | 포인터 배열 + 객체 분산 | 메모리 상 연속된 `x, y, x, y` 배열 |
| 메모리 접근 | 여러 번 캐시 접근 | 1회 연속 접근 가능 |
| GC 작업량 | 높음 | 낮음 (헤더 없음) |

### 💬 실무 예시

- `Point(x, y)`, `Color(r, g, b)`, `Money(amount, currency)` 등 자주 쓰이는 단순 구조체
- 대량 계산, 렌더링, 좌표 변환, JSON 직렬화 시 **flattened layout이 매우 유리**

---

## ✅ 4. 🚀 ZGC (Generational)과 메모리 효율

> ❓ G1 GC와 비교 시 ZGC의 강점은?

### 🌱 ZGC의 특징

| 항목 | 설명 |
| --- | --- |
| **Low Pause GC** | STW(Pause) 시간 1~2ms 수준 |
| **큰 힙 처리** | 수십 GB~1TB도 가능 |
| **GC 동작 원리** | 병렬/비동기 처리로 Pause 없이 마킹/스윕 진행 |

### 🧠 Generational ZGC (JDK 24 기본 적용)

| 세대 분리 | 효과 |
| --- | --- |
| Young / Old Generation 구분 | - 단기 객체 중심 GC 실행 → 더 빠름- 장기 객체는 덜 자주 검사 |

### 📈 G1 GC vs ZGC 비교

| 항목 | G1 GC | ZGC |
| --- | --- | --- |
| 지연 시간 | 중간 (tens~hundreds ms) | 매우 낮음 (<10ms) |
| Throughput | 높음 | 낮음~중간 |
| 대형 힙 | 가능하지만 GC 시간 증가 | 효율적 처리 |
| 서비스 유형 | 일반 웹 API, JVM기반 서비스 | **Latency 민감 서비스**, 대형 배치 시스템 등 |

---

## ✅ 5. 🧱 Compact Object Header의 메모리 최적화 효과

> ❓ 헤더가 작아지면 왜 성능이 올라갈까?

### 🧩 객체 헤더란?

Java의 모든 객체는 **숨겨진 헤더(12바이트 또는 8바이트)** 를 가짐.

#### 역할
- 클래스 타입 정보
- `hashCode()` 저장
- GC 관련 정보
- 동기화(monitor) 정보

### ⚙️ Compact Headers (JDK 24 실험적)

| 항목 | 기존 | Compact 적용 시 |
| --- | --- | --- |
| 헤더 크기 | 96비트 | 64비트 |
| 평균 객체 크기 | 32~64바이트 | 헤더 비율 ↓ → 전체 메모리 ↓ |

### 📈 실무에서 기대할 수 있는 효과

- 수백 수천만 개 객체를 다루는 경우 → 메모리 사용량 **10~~20% 감소**
- 캐시 라인 충돌 감소 → L1/L2 Cache 적중률 증가 → 연산 성능 향상
- 특히 **String, DTO, Event 객체**처럼 **작지만 반복되는 객체**에 효과 큼

```java
// 실습 : Compact Object Header 메모리 비교
// JOL(Java Object Layout) 사용 예시

import org.openjdk.jol.info.ClassLayout;

public class JolExample {
    static class NormalObject {
        int x;
        int y;
    }

    public static void main(String[] args) {
        NormalObject obj = new NormalObject();
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
    }
}
```