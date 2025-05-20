아래는 Per(Oracle Java Core Library 팀 소속)의 유튜브 발표 **“Java Performance Update”** 전체 내용을 바탕으로 구성한 **종합 상세 요약**입니다. 발표는 Java의 최신 성능 개선, 내부 최적화 전략, 성능 측정 기법, JDK 주요 프로젝트들의 기여, 실 사례, 미래 계획 등으로 구성되어 있습니다.

---

## 🎬 발표 개요

- **발표자**: Per, Oracle Java Core Library 팀 소속
- **주제**: 자바의 성능 개선과 관련된 내부 전략, 실 구현 사례, 향후 로드맵
- **주요 대상**: Java 성능에 관심 있는 개발자, 시스템 프로그래머, JVM 최적화에 관여하는 엔지니어

---

## 🧩 1. 오픈JDK 주요 프로젝트와 성능 기여 (00:01–03:55)

| 프로젝트명 | 내용 및 성능 관련 기여 |
| --- | --- |
| **Amber** | 언어 기능 개선 → 더 간결하고 최적화 유도 코드 가능 |
| **Babylon** | 코드 리플렉션 고도화 → GPU 코드 생성 가능 |
| **Leiden** | CDS 활용, **시작 속도 최대 4배 향상** |
| **Loom** | 가상 스레드 → 수백만 개 생성 가능 → **서비스 확장성 향상** |
| **Panama** | Foreign Function & Memory API + 벡터 연산(SIMD) 지원 |
| **Valhalla** | Value Class (래퍼 없는 원시형) → **메모리/성능 개선** |
| **ZGC** | 정지 시간 없는 GC, 대용량 힙에서도 효율적 수집 가능 |

---

## 🧪 2. 성능 측정 기준 및 방법 (05:03–09:14)

### ⏱ 시간 기반 메트릭

- **처리량(Throughput)**: 초당 연산 횟수
- **지연시간(Latency)**: 평균/중앙값/최악의 경우 (e.g., P50, P99, P99.999 등)
- **시작 시간(Start-up Time)**: 앱 실행 ~ 첫 요청 처리까지 시간
- **워밍업(Warm-up)**: JIT 컴파일 후 최고 성능 도달까지 시간

### 🔁 리소스 기반 메트릭

- **메모리 사용량**, **캐시 접근 패턴**, **스레드 수**, **연산 병렬도**, **전력 효율**

---

## ⚙️ 3. 벤치마킹 실전 가이드 (11:11–13:37)

- 노트북에서 성능 테스트 ❌ → **전용 서버 권장**
- **JMH(Java Microbenchmark Harness)** 사용:
    - @Benchmark 애너테이션 적용
    - 반복 횟수, 워밍업, 오차 범위 자동 측정
    - 결과 소비(consume) 및 JIT 최적화 방지 지원
- `System.nanoTime()` 사용 → 시간 왜곡 방지 (`currentTimeMillis()`는 시스템 보정 영향 있음)

---

## 🔄 4. JVM 최적화 흐름 (Tiered Compilation) (18:48–20:44)

| 단계 | 설명 |
| --- | --- |
| Tier 0 | 바이트코드 해석 실행 (느림) |
| Tier 1~3 | 프로파일 수집 (메소드 호출 횟수, 파라미터 유형 등) |
| Tier 4 (C2/Graal) | **최적화 컴파일**: speculative 최적화 + uncommon trap으로 복귀 가능성 확보 |

---

## 🛠️ 5. C2 컴파일러 최적화 전략 (21:29–27:01)

### 주요 기법 예시 (Copy 함수 기준):

1. **Inlining**: 함수 호출 제거
2. **Loop Unrolling**: 루프 반복 4배 단위 → 반복 비용 감소
3. **Hoisting**: 범위 체크 등 루프 외부로 이동
4. **Autovectorization**: 바이트 → 롱(Long) → 256비트 SIMD 단위로 병렬 처리
5. **SIMD 및 Alignment**:
    - 64비트 정렬 후 성능 강화
    - **Pre/Post Alignment**: head/tail 처리로 데이터 정렬 처리

---

## 🧨 6. 성능 저하 원인 대응 (27:38–32:10)

| 문제 | 설명 및 대응 |
| --- | --- |
| **Aliasing** | 동일 메모리 영역 복사 시 값 덮어쓰기 오류 → 보수적 접근 |
| **Control Flow** | 루프 내 조건문 존재 시 최적화 불가 → 일반 루프 처리로 fallback |

---

## 🚀 7. 최근 성능 개선 사례 (Java 23~25) (32:48–45:30)

### 📌 주요 사례 정리:

| 항목 | 설명 | 성과 |
| --- | --- | --- |
| **1. Merged Store** | Long → Byte 배열 저장 시 패턴 인식 후 단일 명령어로 최적화 | 최대 8배 개선 |
| **2. Memory Segment API Java 구현** | Native 대신 Java로 대체 | Native보다 빠름 (특히 작은 데이터) |
| **3. Supertype Cache 개선** | 1-요소 캐시 → 불안정 → 불변 테이블 도입 | 멀티스레드 시 성능 안정성 확보 |
| **4. String Concatenation 최적화** | 메서드 핸들 대신 히든 클래스 재사용 | 시작 시간 40% ↓, 생성 코드 50% ↓ |
| **5. Generational ZGC 기본 적용** | 객체 수명 기반 GC 전략 | GC 성능 대폭 향상 |
| **6. Compact Object Headers** | 객체 헤더 96bit → 64bit | 메모리 사용량 최대 20% ↓, 캐시 효율 ↑ |

---

## 🧭 8. 향후 로드맵 및 실험적 기술 (45:30–47:22)

| 프로젝트/기술 | 설명 |
| --- | --- |
| **Leiden** | AOT 방식 클래스 로딩 → 즉시 부팅 가능 |
| **Valhalla** | Point(x, y)와 같은 구조체 → 메모리 플랫하게 저장 |
| **Vector API** | SIMD 성능 활용 → 9차례 인큐베이팅 중 |
| **Glomm** | 가상 스레드 핸들링 개선 + Pinning 최소화 |
| **Stable Values** | Lazy 초기화 + 컴파일 시 상수처럼 최적화 가능 |
| **Stream Gatherers** | 기존 Stream보다 빠른 구현 가능 |

---

## 🧪 9. 성능 테스트 방법 안내

- 최신 빌드(Loom, Valhalla 포함)는 [https://jdk.java.net](https://jdk.java.net/) 에서 다운로드 가능
- **자신의 애플리케이션을 최신 JVM에서 실행해 비교 테스트**해보는 것 권장

---

## 📚 부록: JMH 기본 예시 코드

```java
java
복사편집
@Benchmark
public int copyTest() {
    int[] src = new int[1024];
    int[] dst = new int[1024];
    for (int i = 0; i < src.length; i++) {
        dst[i] = src[i];
    }
    return dst[0];
}

```

---

## ✅ 요약 정리

- Java는 지속적인 최적화를 통해 **성능/메모리/전력 효율**을 극대화 중
- JMH, nanoTime, JIT 이해, C2 최적화 기법 숙지가 중요
- 프로젝트 Amber, Valhalla, Leiden 등은 단순 문법 이상의 **성능 간접 유도 효과**를 제공
- 최신 JVM으로 애플리케이션 성능 테스트 권장
