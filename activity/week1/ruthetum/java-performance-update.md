# Java Performance Update
> https://www.youtube.com/watch?v=rXv2-lN5Xgk

## 개요
- Java의 버전이 업데이트됨에 따라 성능이 개선되었으며, 22, 23은 21 보다 성능이 개선됨
- 영상에서는 Java 코드의 라이프 사이클과 함께 주요 성능 개선 사항(코드 최적화)을 다룸
  - Inline
  - Loop Unrolling
  - Hoisting
  - Auto Vectorization
  - Pre Alignment, Post Alignment
  - Long-to-Int looping conversion

<br/>

## 들어가기에 앞서 
Java에서 여러 프로젝트가 진행되면서 성능이 개선됨
- [Amber](https://openjdk.org/projects/amber/): 새로운 언어 기능(일종의 사용자와의 인터페이스, 패턴)를 제공 (성능을 직접적으로 개선하지는 않음
  - https://www.baeldung.com/java-project-amber
- [Babylon](https://openjdk.org/projects/babylon/): GPU와 같은 외부 프로그래밍 모델로 Java를 확장하기 위한 목표, 코드 리플렉션을 분석할 수 있는 기능 제공
- [Leyden](https://openjdk.org/projects/leyden/): Java의 startup 및 warmup 시간을 개선하기 위한 목표
- [Loom](https://wiki.openjdk.org/display/loom/Main): 경량 스레드(가상 스레드)를 통해 서비스 확장성 개선
- [Panama](https://openjdk.org/projects/panama/): Java에서 C/C++과 같은 네이트브 코드와 호환을 지원, 외부 함수 및 메모리 API 제공
  - https://www.baeldung.com/java-project-panama 
- [Valhalla](https://openjdk.org/projects/valhalla/): 값 기반 클래스 및 메모리 표현 개선을 통해 성능과 메모리 사용량 향상

그 외 GC 개선도 이루어짐

<br/>

## 성능 평가
성능 평가를 위해 여러 지표를 고려해야 해야함
- 평균 처리량과 지연 시간
- startup 및 warmup 시간
- resource 지표 (e.g. CPU, 메모리 사용량)

그 외에 에너지 효율성, 플랫폼의 특징 등도 고려해야 함

<br/>

## 벤치마킹 도구
> JMH 궁금하신 분들은 [이 글](https://github.com/sipe-team/3-1_spurt/tree/main/playground/ruthetum/practice/benchmark) 참고

- 단순히 시간으로 측정하는 경우 오차가 발생할 수 있음
  - `System.nanoTime`을 사용하는 경우 실제 시간과 오차가 발생할 수 있음
- JMH는 코드 인라인을 방지하고 입력 데이터를 제거함으로써 실제 코드를 테스트하도록 보장
- 또한 여러 VM에서 여러 라운드를 실행하여 JIT 컴파일러의 영향을 관찰할 수 있음
- 성능 측정 시 warmup 시간을 결정할 수 있으며, 오차의 범위를 추정할 수 있음

<br/>

## Java 코드의 생명 주기와 JIT 컴파일러

![image](https://www.baeldung.com/wp-content/uploads/2021/07/3.png)

> - Interpreter: TO
> - C1 Compiler: T1 ~ T3
> - C2 Compiler: T4

- Java 코드는 JVM 내에서 실행될 때 JVM은 코드를 우선적으로 인터프린팅함 (속도 느림)
  - JIT 컴파일러는 warmup 단계가 끝난 후 시작하여 런타임에 핫 코드를 컴파일
  - 여기서 수집된 프로파일링 정보를 사용하여 최적화를 수행
- C1 컴파일러는 가능한 빠른 실행 속도를 위해 코드를 가능한 빠르게 최적화하고 컴파일 (여기서도 프로파일링 데이터 수집)
  - 특정 메서드가 C1 컴파일러의 임계치 설정 이상으로 호출되면, 해당 메서드의 코드는 C1 컴파일러를 통해 제한된 수준으로 최적화
  - 이후 컴파일된 기계어는 코드 캐시에 저장
- C2 컴파일러는 컴파일 시간이 C1 컴파일 시간보다 상대적으로 길지만 더 높은 수준의 최적화를 지원
  - 최적화와 컴파일이 끝나면 마찬가지로 코드 캐시에 기계어를 저장

<br/>

## 코드의 최적화 전략
### Inlining
- 메소드 호출을 제거하고 호출되는 메소드의 코드를 호출 위치에 직접 삽입
- 이를 통해 메소드 호출에 따르는 오버헤드 감소

### Loop Unrolling
- 루프의 반복 횟수를 줄이고 한 번의 반복문에서 여러 작업을 수행
- 이를 통해 루프 제어에 필요한 비용을 줄이고 병렬 처리를 용이하게 함

### Hoisting
- 반복문 내에서 반복적으로 수행될 필요가 없는 연산을 루프 밖으로 이동시킴
- 예를 들어 배열 크기에 대한 확인(ArrayIndexOutOfBoundsException)를 루프 시작 전에 한 번만 수행할 수 있음

### Auto Vectorization
> 이해하기로는 직렬(차례)로 처리될 연산을 병렬(동시)로 처리할 수 있도록 변환하는 것
- SIMD(Single Instruction, Multiple Data) 명령어를 활용해서 여러 데이터 요소에 대한 연산을 한 번에 수행
  - 이를 통해 데이터 처리량이 증가함
- 호이스팅 및 언롤링과 함께 적용되어 추가적인 성능 향상 가능

### Pre Alignment, Post Alignment
- 메모리 접근 시 데이터가 CPU 캐시 라인에 정렬되도록 앞부분(Pre)과 뒷부분(Post)을 바이트 단위로 처리
- 중간 부분은 워드(word) 또는 벡터 단위로 효율적으로 처리

### Long-to-Int looping conversion
- 64비트 메모리 주소를 사용하는 long 인덱스 대신 32비트 int 인덱스를 사용하는 중첩 반복문 구조로 변환하여 성능을 향상 (CPU가 32비트 연산을 더 빠르게 처리할 수 있음)

<br/>

## 그 외 적용됐거나 앞으로 버전에서 주요 성능 개선 사항

|          **기능 및 개선**          | **효과**                                                                                           |
|:-----------------------------:|--------------------------------------------------------------------------------------------------|
|          MergeStore           | JIT 컴파일러 기능으로, 여러 배열 쓰기 연산을 하나의 대용량 연산으로 병합. C2 컴파일러가 패턴 인식하여 자동 적용. Offset을 변경하지 않는 경우에 효과적     |
|    외부 함수 메모리 API 벌크 연산 개선     | FFM API의 fill, copy, mismatch 등 대용량 연산 구현을 네이티브 코드에서 Java 코드로 전환하여 성능 향상. 작은 크기에서는 Java 코드가 더 빠름 |
| Secondary Super Cache 스케일링 개선 | `instanceof` 등 슈퍼타입 쿼리 성능 향상. 기존 1개 요소 캐시의 문제를 해결하기 위해 불변 조회 테이블 도입                              |
|           문자열 연결 개선           | startup 성능 향상. 메소드 핸들 조합기 대신 숨겨진(hidden) 클래스를 직접 생성하여 효율성 증대                                     |
|   Generational ZGC 모드 기본 설정   | ZGC가 Generational 모드로 기본 동작. 객체를 신규 세대와 구형 세대로 나누어 GC 성능 개선. 대부분의 객체가 짧은 수명을 가진다는 점을 활용          |



## 그 외 볼만한 자료
### JIT 컴파일러와 C1/C2 컴파일러
- https://velog.io/@kimunche/C1C2-Compiler-%EC%99%80-JIT-Compiler
- https://www.baeldung.com/jvm-tiered-compilation

### Generational ZGC
- https://inside.java/2023/11/28/gen-zgc-explainer/
- https://sungjk.github.io/2024/06/29/zgc.html

### GC와 STW 부하테스트
- https://stir.tistory.com/542

### Auto Vectorization
- https://daniel-strecker.com/blog/2020-01-14_auto_vectorization_in_java/