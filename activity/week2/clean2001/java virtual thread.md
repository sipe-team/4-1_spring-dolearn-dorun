## 영상 요약
영상 듣고 블로그 포스팅 하였습니다!
https://onfonf.tistory.com/131


## +추가: VirtualThread와 기존 Java Thread의 성능 비교 테스트 코드
```java
package org.example.dorun.study;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class VirtualThreadTest {

    private static final int TASK_COUNT = 10_000;
    private static final int SLEEP_MILLIS = 100;

    public static void main(String[] args) throws Exception {
        System.out.println("Running with " + TASK_COUNT + " tasks, each sleeping for " + SLEEP_MILLIS + "ms\n");

        runWithVirtualThreads();
        runWithFixedThreadPool();
    }

    private static void runWithVirtualThreads() throws Exception {
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < TASK_COUNT; i++) {
            futures.add(virtualExecutor.submit(() -> {
                try {
                    Thread.sleep(SLEEP_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();  // 모든 작업 완료 대기
        }

        long end = System.currentTimeMillis();
        System.out.println("✅ Virtual Threads: " + (end - start) + " ms");

        virtualExecutor.shutdown();
    }

    private static void runWithFixedThreadPool() throws Exception {
        ExecutorService fixedExecutor = Executors.newFixedThreadPool(100);
        List<Future<?>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < TASK_COUNT; i++) {
            futures.add(fixedExecutor.submit(() -> {
                try {
                    Thread.sleep(SLEEP_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();  // 모든 작업 완료 대기
        }

        long end = System.currentTimeMillis();
        System.out.println("✅ Fixed Thread Pool (100 threads): " + (end - start) + " ms");

        fixedExecutor.shutdown();
    }
}
```

- 로컬 실행 결과
```
오후 8:50:17: Executing ':org.example.dorun.study.VirtualThreadTest.main()'…

> Task :compileJava
> Task :processResources UP-TO-DATE
> Task :classes

> Task :org.example.dorun.study.VirtualThreadTest.main()
Running with 10000 tasks, each sleeping for 100ms

✅ Virtual Threads: 182 ms
✅ Fixed Thread Pool (100 threads): 10388 ms

BUILD SUCCESSFUL in 11s
3 actionable tasks: 2 executed, 1 up-to-date
오후 8:50:29: Execution finished ':org.example.dorun.study.VirtualThreadTest.main()'.
```

- 이런 결과가 나온 이유
  - Virtual Thread는 태스크 1개당 1개의 스레드로 동시에 실행 가능하다. (개수 제한 없음)
  - FixedThreadPool은 최대 100개의 커널 스레드로 제한.
  - sleep처럼 블로킹 작업이 많은 경우 Virtual Thread의 효율성이 잘 보임

----

## VirtualThread의 사용

- Thread.startVirtualThread(Runnable task);
- Thread.ofVirtual.start(Runnable task);
- ExecutorService.submit(() -> { })

```java
package org.example.dorun.study;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleVirtualThreadExample {
    // 기본적인 버츄얼 스레드 생성 1
    void createVirtualThreadWithLambda() {
        Thread.startVirtualThread(() -> {
            System.out.println("hi!");
        });
    }
    
    // 기본적인 버츄얼 스레드 생성 2
    void createVirtualThreadWithRunnable() {
        Runnable runnable = () -> {
            System.out.println("hi!");
        };
        
        Thread virtualThread = Thread.ofVirtual()
                .name("my_thread1", 1)
                .unstarted(runnable);
        
        virtualThread.start();
    }
    
    // ExecutorService를 사용하여 버추얼 스레드 생성
    void startVirtualThread() {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        executorService.submit(() -> {
            System.out.println("ExecutorService VirtualThread");
        });
        
        executorService.shutdown();
    }
}
```

- Reference: https://0soo.tistory.com/259