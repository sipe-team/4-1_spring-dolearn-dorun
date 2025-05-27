# [4월 우아한테크세미나] ‘Java의 미래, Virtual Thread’ 발표 정리

## 표본

- 제목: Java의 미래, Virtual Thread
- 방송 사직: 우아한테크세미나 4월
- 보기: [영상 연결고리](https://www.youtube.com/watch?v=BZMZIM-n4C0)
- 발표자: 우한형제들 김태원 (파일서버 개발자)

---

## 시작점: Virtual Thread는 "준다" 판정이 아니다

- Virtual Thread는 무조건 다 좋다고 바로 적용하는 기술이 아니다.
- 필요에 맞는 사항, 건설, 노드 형태에 따라 확인해야 한다.

---

## 운원방식이 다른 Virtual Thread 가격 점

### 1. 느리지 않은 생성/스케줄 비용

- **Thread Pool 사용 필요 X**
    - 기존 자바 Thread: OS 스레드를 만들기 위해 무거운 System Call 이 발생
    - **Virtual Thread**: JVM 내 스케줄링 되어 OS 가 도움을 안 쓰면 됨

```java
// 기존 Thread
Thread t1 = new Thread(() -> {});
t1.start();

// Virtual Thread
Thread vt = Thread.startVirtualThread(() -> {});
```

- 효율적 테스트:
    - 100만개 Thread 생성:
        - Platform Thread: 다음 시간이 필요
        - Virtual Thread: 너무 빠른 시간 (대단적 성능과 속도)

---

### 2. 논블로킹 I/O 지원

- 실습적 테스트:
    - 시도: 10초 속도의 API 100회 동시 호출
    - 결과:
        - 기존 Thread: 130초
        - Virtual Thread: 101초
- 이유:
    - Virtual Thread 는 I/O 블록 시 JVM 내부의 **Continuation Yield** 를 통해 스크린을 중단하고 다음 Task 지정

---

### 3. 기존 Java 앱과 100%확인 호환

- `Thread`, `Runnable`, `ExecutorService` 등과 그대로 원하는 대요:

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> System.out.println("Hello Virtual Thread"));
```

- 필수 신규 데이터는 없음. 기존 Java 소스 사용가능

---

## Virtual Thread 동작 원리 상세 해부

### 4. 스케줄링 구조

- **기존 Thread**: OS 커널 Thread 생성 필요
- **Virtual Thread**:
    - JVM ForkJoinPool 의 "Carrier Thread" 사용 (1:N)
    - 시스템 커널 업체 없음
    - ForkJoinPool 또한 Work-Stealing 모델 사용

```java
// 시작 시 Carrier Thread 업데이트
private static final ForkJoinPool DEFAULT_SCHEDULER = ForkJoinPool.commonPool();
```

---

### 5. Continuation 단위 사용

- 중단 및 재시작 가능
- 예:

```java
ContinuationScope scope = new ContinuationScope("example");
Continuation cont = new Continuation(scope, () -> {
    System.out.println("Start");
    Continuation.yield(scope);
    System.out.println("Resume");
});

cont.run(); // Start
cont.run(); // Resume
```

- Virtual Thread 내부에 이를 Task 단위로 사용

---

### 6. Tomcat 연동 예제

- 적용 포인트:

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer() {
    return factory -> factory.addConnectorCustomizers(connector -> {
        connector.getProtocolHandler().setExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    });
}
```

- Virtual Thread 포탈 적용으로 만족한 노드 조정 가능

---

## 성능 비교 테스트

| 테스트 | Platform Thread | Virtual Thread |
| --- | --- | --- |
| IO Bound TPS | 100 TPS | **151 TPS** |
| CPU Bound TPS | 100 TPS | **93 TPS** |
- IO에서 성능 유니터 51% 증가
- CPU Bound에서 오픈한 이유:
    - 스케줄링 등 부가가 복잡해지기 때문

---

## 적용 시 주의사항

### 1. Lock Pinning 문제

- `synchronized`, native method 사용 시 Carrier Thread 블록 발생
- 해결: `ReentrantLock` 로 대체

### 2. Pooling 금지

- 무조건 생성/삭제하는 방식이므로 풀을 만들면 성능 숨짐

### 3. CPU Bound 작업 부적합

- 일반 스레드보다 비용 높음

### 4. ThreadLocal 주의

- Virtual Thread 는 반복 생성 되므로 특정 크기 데이터를 붙여서 활용하면 성능 피해

### 5. 리소스 제한

- DB Connection Pool 같은 유형 자원은 **무조건 Virtual Thread** 생성으로 때 정해지는 상황 발생
- Semaphore, 노드 다루기 전 노드 사용 제한 필요

---

## WebFlux / Kotlin Coroutine 비교

### 공통점

- 둘 다 **논블로킹 I/O** 모델 구현 목표
- 기존 Thread-per-request 방식의 대안 제공

### 차이점

| 항목 | Virtual Thread (Java) | WebFlux (Reactor) | Kotlin Coroutine |
| --- | --- | --- | --- |
| 동작 방식 | Thread 단위 생성 | EventLoop + Callback | Suspend 함수로 중단 가능 |
| 러닝 커브 | 낮음 (기존 Java와 동일) | 높음 (함수형 개념 필요) | 중간 (DSL 익숙해야 함) |
| 디버깅 편의성 | 높음 (Stack trace 유지) | 낮음 (비직관적 흐름) | 비교적 높음 |
| 구조화된 동시성 지원 | 미지원 (예정) | 지원 | 지원 (CoroutineScope) |
| 코드 예시 | 기존 코드 그대로 사용 가능 | Flux/Mono API 사용 필요 | suspend 함수 기반 사용 |

### 예제 비교

### Virtual Thread

```java
ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
exec.submit(() -> {
    String result = callBlockingAPI();
    System.out.println(result);
});
```

### WebFlux (Reactor)

```java
java
복사편집
Mono.fromCallable(() -> callBlockingAPI())
    .subscribe(System.out::println);

```

### Kotlin Coroutine

```kotlin
GlobalScope.launch {
    val result = callBlockingAPI()
    println(result)
}
```

---

## 스터디용 요점 정리 (두런두런)

> 공유하면 좋을 세 가지 주제

### 1. Virtual Thread vs ThreadPool 성능 비교 실습

- 목적: 기존 `ThreadPool` 기반 처리 방식과 `Virtual Thread` 간의 실제 처리량(TPS), 메모리 사용량, 응답 시간 비교
- 예제 코드:

```java
public class ThreadPoolVsVirtualThread {
    static final int TASK_COUNT = 1000;

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService fixedPool = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            fixedPool.submit(() -> {
                simulateBlockingCall();
                latch.countDown();
            });
        }
        latch.await();
        fixedPool.shutdown();
        System.out.println("FixedThreadPool Time: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor();
        latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            virtualPool.submit(() -> {
                simulateBlockingCall();
                latch.countDown();
            });
        }
        latch.await();
        virtualPool.shutdown();
        System.out.println("VirtualThread Time: " + (System.currentTimeMillis() - start));
    }

    static void simulateBlockingCall() {
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
    }
}
```

---

### 2. ThreadLocal 사용 시 메모리 이슈 실험

```java
public class ThreadLocalLeakTest {
    private static final ThreadLocal<byte[]> local = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 1_000_000; i++) {
            executor.submit(() -> {
                local.set(new byte[1024 * 1024]); // 1MB 할당
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        System.out.println("Done");
    }
}
```

- 설명:
    - ThreadLocal은 쓰레드 종료 이후 수거되지 않으면 힙 메모리에 유출 가능
    - Virtual Thread는 반복 생성되므로 이 영향이 더욱 민감함

---

### 3. Tomcat에 Virtual Thread 적용과 동작 확인

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer() {
    return factory -> factory.addConnectorCustomizers(connector -> {
        connector.getProtocolHandler().setExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    });
}
```

```java
@RestController
public class SampleController {
    @GetMapping("/sleep")
    public String sleep() throws InterruptedException {
        Thread.sleep(1000);
        return "Slept";
    }
}
```

- 확인 포인트:
    - 동시 호출 시 CPU / 메모리 부하 모니터링
    - 일반 Tomcat과 성능 비교 (동시 1000건 호출 등)
    - I/O bound 상황에서의 이점 확인

---

## 결론

> “Virtual Thread 는 JVM 내에서 실행되는 것을 전제로, 항상 적용 전 성능 비교 와 리소스 테스트가 수반되어야 한다.”

### 1. 가벼운 생성/스케줄 비용

- **설명**: Virtual Thread는 생성할 때마다 실제 OS 스레드를 만들지 않고 JVM 내부의 **Carrier Thread** 위에서 경량 실행 단위로 작동함.
- **장점**:
  - ThreadPool이나 스레드 재사용 없이도 수십만 개의 동시 요청 처리 가능
  - context switching 비용 절감
- **비교 예제**:

```java
public class CreationBenchmark {
    public static void main(String[] args) {
        int count = 1_000_000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Thread t = new Thread(() -> {});
            t.start();
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Platform Threads: " + duration + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Thread.startVirtualThread(() -> {});
        }
        duration = System.currentTimeMillis() - start;
        System.out.println("Virtual Threads: " + duration + "ms");
    }
}
```

- **예상 결과**: Virtual Threads는 Platform Threads 대비 수십 배 빠른 생성 시간 보장

---

### 2. Continuation 기반 지원 → 논블로킹 I/O 유리

- **설명**: Virtual Thread는 JVM이 **Continuation(작업 단위의 중단/재개)**를 직접 관리하면서 I/O 대기 시간 동안 다른 작업 수행 가능
- **장점**:
  - 비동기/Reactive 코딩 없이도 논블로킹 효과 실현 가능
  - 코드를 직관적인 동기식 흐름으로 작성 가능
- **예제 코드**:

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        try {
            System.out.println(Thread.currentThread() + " sleeping...");
            Thread.sleep(2000); // 블로킹 I/O
            System.out.println(Thread.currentThread() + " awake!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}
```

- **설명**: 위 코드는 동기식 `Thread.sleep()`을 사용했지만, Virtual Thread는 해당 작업을 yield 처리하여 다른 Task 실행 가능

---

### 3. 기존 Java 조건과 호환 잘 됨

- **설명**: `Thread`, `Runnable`, `ExecutorService`, `synchronized` 등 대부분 기존 코드 구조를 수정 없이 그대로 사용 가능
- **기술적 기반**: Virtual Thread는 `Thread` 클래스를 상속하므로, 리스코프 치환 원칙(Liskov Substitution Principle)을 만족함
- **예제**:

```java
ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
Future<String> result = service.submit(() -> {
    Thread.sleep(500);
    return "완료됨";
});
System.out.println(result.get());
```

- **설명**: 기존 Java Future 기반 API 그대로 사용 가능, 특별한 Reactive 환경이나 DSL 불필요

---

### 4. 최대 사용 전 모든 조건 명확히 판단 필요

- **주의사항 정리**:
  - IO Bound 작업에 적합 / ❌ CPU Bound는 오히려 역효과 가능
  - ThreadPool 사용 금지 / ❌ ThreadLocal 등 상태 보존 로직 사용 시 메모리 유출 주의
  - DB 커넥션, 파일 핸들 등 유한 자원 사용 시 Semaphore 등으로 명시적 조절 필요
- **도입 체크리스트 코드 예시**: 세마포어를 통한 DB 커넥션 보호

```java
Semaphore dbConnectionLimit = new Semaphore(100); // 최대 100개의 커넥션 허용

ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

for (int i = 0; i < 1000; i++) {
    service.submit(() -> {
        try {
            dbConnectionLimit.acquire();
            simulateDatabaseCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            dbConnectionLimit.release();
        }
    });
}

void simulateDatabaseCall() {
    try {
        Thread.sleep(100); // DB 처리 대기
    } catch (InterruptedException ignored) {}
}
```

- **설명**:
  - Virtual Thread가 무제한 생성된다고 해도, 리소스는 유한함
  - 외부 시스템(DB 등) 연동 시 반드시 제어가 필요함