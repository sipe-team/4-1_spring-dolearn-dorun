# Java의 미래, Virtual Thread

## Virtual Thread란?

- 2018년 Project Loom으로 시작된 경량 스레드 모델
- 2023년 JDK21에 정식 Feature로 추가

## Virtual Thread 장점

### 1. 저렴한 스레드 생성 및 스케줄링 비용

기존 Java 스레드의 문제점:

- 스레드 생성 및 스케줄링 비용이 크기 때문에 스레드 풀 사용
- 스레드당 최대 2MB 메모리 사용
- OS에 의한 스케줄링으로 system call 오버헤드 발생

이러한 문제 해결을 위해 Virtual Thread가 만들어짐

- 스레드 풀이 필요 없으며, 요청이 들어올 때마다 생성 및 소멸
- 메모리 사용량이 매우 낮음 (50KB 이하)
- JVM 내부에서 스케줄링하여 system call 오버헤드 없음

정리하면,

|             | thread | virtual thread |
|-------------|--------|----------------|
| 메모리 사이즈     | ~2MB   | ~50KB          |
| 생성 시간       | ~1ms   | ~1µs           |
| 컨텍스트 스위칭 시간 | ~100µs | ~10µs          |

### 2. Non-blocking I/O 지원

- Virtual Thread는 JVM 내에서 Continuation을 사용하여 Non-blocking I/O 처리
- 기존 thread per request 모델의 병목 문제 해결

### 3. 기존 코드와 높은 호환성

- Virtual Thread는 기존 Thread 클래스를 상속 받아 사용

  ```java
  final class VirtualThread extends BaseVirtualThread {
  }
  
  sealed abstract class BaseVirtualThread extends Thread {
  }
  ```

- ExecutorService를 Virtual Thread로 쉽게 전환 가능

  ```java
  
  @Bean
  public ExecutorService executorService() {
      return Executors.newVirtualThreadPerTaskExecutor();
  }
  ```

## Virtual Thread 구조와 동작 원리

### 기존 Thread 구조

- 플랫폼 스레드
- OS에 의해 스케줄링
- 커널 스레드와 1:1 매핑
- 작업 단위 Runnable

<img width="550" alt="Image" src="https://github.com/user-attachments/assets/20157ecc-5a57-4b4a-a0d2-3a32978ddae3" />

### Virtual Thread 구조

- 가상 스레드
- JVM에 의해 스케줄링
- 캐리어 스레드와 1:N 매핑
- 작업 단위는 Continuation

<img width="658" alt="Image" src="https://github.com/user-attachments/assets/9a52a3d8-96d4-485f-8a16-5bb6111cb496" />

#### Continuation

- 중단 및 재실행 가능한 작업 흐름
- 중단된 작업은 Heap 영역에 저장하며, 실행 중인 작업은 Stack 영역에서 처리함

<img width="406" alt="Image" src="https://github.com/user-attachments/assets/293d32ce-28cd-42b3-8e79-6ddea7b65268" />

- Virtual Thread 작업 중단을 위해서는 Contiunation yield 메서드를 호출

```java
public static void main(String[] args) {
    ContinuationScope continuationScope = new ContinuationScope("Virtual Thread");
    Continuation continuation1 = Continuation.create(continuationScope, () -> {
        System.out.println("Continuation1 : 실행 중 1");
        Continuation.yield(continuationScope); // 중단
        System.out.println("Continuation1 : 실행 중 2");
    });
}
```

- Virtual Thread 작업을 중단할 때는 `LockSupport.park()` 메서드를 사용
- 기존에는 `Unsafe.park()`를 호출하여 실제 스레드 중단
- JDK21부터는 `Continuation.park()`를 호출하여 Virtual Thread만 중단

## 적용 시 주의 사항

### 1. Pin 현상 (Blocking carrier thread)

- 캐리어 스레드를 blocking 하면 Virtual Thread 활용 불가
  - synchronized, parallelStream 등
- VM 옵션으로 감지 가능 : `-Djdk.tracePinnedThreads=short,full`
- 병목 가능성이 존재하므로 사용 라이브러리 release 버전에 대한 점검 필요
- 변경 가능하다면 `java.util`의 `ReentrantLock` 사용

### 2. No Pooling

- 생성 비용이 저렴함 (별도의 pooling 불필요)
- 사용할 때마다 생성하고 GC 처리

### 3. CPU Bound Task
- 캐리어 스레드 위에서 동작하므로 성능 낭비 발생
- Non-blocking 장점을 활용하지 못함

### 4. 경량 스레드

- Thread Local을 최대한 가볍게 유지

### 5. 배압
- Virtual Thread는 배압 개념을 지원하지 않음

## 결론
- 가볍고 빠르며 Non-blocking 방식 지원
- JVM 스케줄링 및 Continuation 기반으로 동작
- Thread per request 모델의 I/O blocking time 병목 문제 해결
- Reactive나 코루틴과 달리 러닝 커브에 부담이 없고 쉽게 적용 가능