# :pushpin: Java Virtual Thread
> https://www.youtube.com/watch?v=BZMZIM-n4C0


### Virtual Thread 소개
- 2018년 Project Loom으로 시작된 경량 스레드 모델
- 2023년 JDK 21에 정식 feature로 추가

- 스레드 생성 및 스케줄링 비용이 기존 스레드보다 저렴
- 스레드 스케줄링을 통해 Nonblocking I/O 지원
- 기존 스레드를 상속하여 코드 호환


### Virtual Thread 장점

실험 1
- 가설: Virtual Thread는 생성 및 실행이 빠르다
- 실험 방법: 스레드 1,000,000개 생성 및 실행

```java
public static void main(String[] args) {
    List<Thread> threads = IntStream.range(0, 1_000_000)
        .mapToObj(i -> new Thread(() -> {}))
        .toList();
    
    threads.forEach(Thread::start);
}
```

> 31.632초 소요


```java
public static void main(String[] args) {
    List<Thread> threads = IntStream.range(0, 1_000_000)
        .mapToObj(i -> Thread.ofVirtual().unstarted(() -> {}))
        .toList();
    
    threads.forEach(Thread::start);
}
```

> 0.375초 소요