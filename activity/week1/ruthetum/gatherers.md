# [Stream Gatherers](https://openjdk.org/jeps/485)
> https://www.youtube.com/watch?v=v_5SKpfkI2U


## 개요
- Gatherers는 Java 22에 도입된 새로운 인터페이스로 24에서 최종 기능으로 포함
- Gatherers는 기존 Stream을 유연하게 조작할 수 있도록 함
  - 기존에 지원됐던 중간 연산 외에 상태에 가반하여 변환을 수행할 수 있으며
  - Collector 연산의 제약을 보완 (무한한 크기의 데이터)
- 인덱스 접근, 윈도우 연산 등 기존 Stream API를 이용하기 어려웠던 부분을 효과적으로 지원


## Gatherers의 특징 및 Collector와의 차이점
- Gatherers는 상태를 유지하며 스트림 요소를 처리할 수 있는 기능을 제공
- Collector는 무한 스트림 등에는 적절하지 않으며, 병렬 처리에 대한 제약이 있음
- Gatherers는 downstream 핸들을 통해 다음 단계로 요소를 push할 수 있는 기능을 제공
  - Gatherers는 조기 종료를 지원하여 불필요한 요소 처리를 피할 수 있음 
- Collector는 최종 결과를 하나만 내보내는 반면, Gatherer는 여러 개의 요소를 내보낼 수 있음
  - Collector는 n:1의 관계를 가지며, Gatherer는 n:m의 관계를 갖음


## Gatherers 주요 메서드

|            메서드             | 설명                                                                                                                                                           |
|:--------------------------:|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
|           `fold`           | 상태를 유지하며 각 입력 요소를 처리하여 새로운 상태를 생성하고, 최종 상태를 다음 단계로 내보냄. `reduce`보다 유연하게 다른 타입의 결과를 생성할 수 있음             |                                                       
|           `scan`           | `fold`과 유사하지만, 각 단계의 중간 결과 상태를 다음 단계로 즉시 내보냄. 누적된 결과를 실시간으로 확인할 때 유용함                                           |                                            
|       `windowFixed`        | 스트림 요소를 고정된 크기의 불변 리스트(윈도우)로 묶어서 내보냄. 스트림의 마지막에 윈도우 크기만큼 요소가 남지 않으면 남은 요소들로 마지막 윈도우를 생성                 |                                                       
|       `windowSliding`      | 스트림 요소를 슬라이딩 윈도우로 묶어서 내보냄. 각 윈도우는 지정된 크기만큼의 요소를 포함하며, 다음 윈도우는 이전 윈도우의 시작 위치에서 지정된 오프셋만큼 이동함 |
|      `mapConcurrent`       | 지정된 `mapper` 함수를 가상 스레드(virtual threads)를 사용하여 동시(concurrently)에 실행. 최대 동시 실행 개수를 제한할 수 있습니다. 스트림 파이프라인의 특정 부분만 동시 처리할 때 유용합니다. 단락 평가를 지원하여 불필요한 작업을 중단할 수 있음 |


### fold
```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

List<String> list = numbers.stream()
    .gather(Gatherers.fold(() -> "", (string, number) -> string + number))
    .toList();

assertEquals(List.of("12345"), list);
```

### scan
```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

List<Integer> list = numbers.stream()
    .gather(Gatherers.scan(() -> 4, Integer::sum))
    .toList();

assertEquals(List.of(4, 5, 7, 10, 14), list);
```

### windowFixed
```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

List<List<Integer>> windows = numbers.stream()
                .gather(Gatherers.windowFixed(2))
                .toList();

assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), windows);
```

### windowSliding
```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

List<List<Integer>> windows = numbers.stream()
        .gather(Gatherers.windowSliding(2))
        .toList();

assertEquals(List.of(List.of(1, 2), List.of(2, 3), List.of(3, 4), List.of(4, 5)), windows);
```

### mapConcurrent
```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

List<Integer> list = numbers.stream()
    .gather(Gatherers.mapConcurrent(1, number -> number * 2))
    .toList();

assertEquals(List.of(2, 4, 6, 8, 10), list);
```


## Gatherers 추가 특징
### andThen
- Gatherers는 `andThen` 메서드를 통해 서로 조합하여 사용할 수 있음
- 간단한 Gatherer들을 연결하여 더 복잡한 중간 연산을 만드는 것을 가능하게 함

### 병렬 처리
- Stream이 병렬 처리를 위해 분할(fork)되면, 각 분할된 Chunk는 개별 태스크로 처리
- 각 태스크는 Gatherer의 initializer로 상태를 초기화하고, integrator를 사용하여 해당 청크의 요소를 순차적으로 처리
- 처리가 완료되면 각 태스크의 부분 결과는 트리 구조를 따라 상위로 결합(join)됨
- `combiner` 메서드는 이 부분 결과 상태를 병합 
- 이후 최종적으로 finisher가 호출되어 최종 결과가 생성 반환 

### Short-circuiting
- Stream의 모든 요소를 처리할 필요 없이, 특정 조건이 만족되면 스트림 처리를 일찍 중단할 수 있음
  - downstream 핸들링(`push`)을 통해 다음 단계로 요소를 전달 (종료 조건)
- 불필요한 계산을 피하고 성능을 최적화할 수 있음