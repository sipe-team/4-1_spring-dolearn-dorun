# Stream Gatherers

![Image.png](Stream%20Gatherers.assets/Image.png)

우리가 필요한 기능은 무엇인가?

- 입출력 비율
    - 중간 작업이 출력이나 입력에 대한 영향을 미칠 수 있어야함
- 무한 스트림 지원 
    - Collector 는 언제 멈춰야할 지 알 수 없으므로 무한 스트림을 지원할 수 없음
    - depth first 처리는 모든 업스트림 요소를 소비한 후에야 다운스트림으로 내보낼 수 있으므로 무한 스트림에서는 불가능합니다.
- 상태 유지(stateful) 또는 상태 비유지(stateless)
    - 요소마다 무언가를 기억해야 할 수도 있고, 그렇지 않을 수도 있음
- 순차적 또는 병렬화 가능
    - 일부 연산(windowing 등)은 요소의 발견 순서(encounter order)에 따라 동작하므로 본질적으로 순차적임
    - Collector는 병렬 평가를 위해 컴바이너(combiner)를 제공해야 하므로 본질적으로 순차적인 연산을 올바르게 처리하기 어려움
- 스트림 끝 콜백
    - 마지막 요소가 처리된 후 최종 결과를 내보낼 수 있도록 스트림 끝을 알 수 있어야 함
        - Collector에는 피니셔(finisher)가 있어 가능

![Image.png](Stream%20Gatherers.assets/Image%20(2).png)

Collector 의 인터페이스 : T 입력 타입, A 누적기/상태 타입, R 결과 타입

- supplier로 상태 생성
- accumulator 로 상태 업데이트
- combiner로 병렬로 결과 병합
- finisher로 최종 결과 생산

위 요구사항에 대한 Collector 평가

- Collector는 N개의 입력을 받아 1개의 결과를 생산하므로, 다른 입출력 비율을 지원하지 못함
- 무한 스트림을 지원할 수 없음
- 상태 유지는 가능(상태 타입이 void가 아니면)
- 조기 종료가 불가능
- 본질적으로 순차적인 연산을 올바르게 처리하기 어렵움 (컴바이너 필요)
- 스트림 끝 콜백은 지원 (피니셔 있음)
-  결론적으로 Collector는 중간 연산의 모든 요구사항을 만족시키지 못함

![Image.png](Stream%20Gatherers.assets/Image%20(3).png)

T : 입력타입, A: 상태 타입 , R : 단일결과가 아닌 Gatherer 의 출력 타입

- initializer: Collector의 supplier와 동일하게 상태 객체를 생성
- integrator: Collector의 accumulator 대신 사용
    - integrator는 integrate 메소드를 가짐
        -  이 메소드는 현재 상태, 다음 요소, 그리고 다운스트림(downstream) 핸들을 인자로 받음
        - 이를 통해 각 요소를 처리하면서 다음 단계(다음 중간 연산)로 요소를 push 할 수 있음
        - Collector의 accumulator는 푸시할 곳이 없어 누적만 할 수 있었음
- combiner: Collector와 동일
- finisher: Collector와 약간 다름
    - Collector의 finisher는 Function이었지만, Gatherer의 finisher는 BiConsumer(상태와 다운스트림 핸들)임. 이는 결과로 하나 이상의 요소를 생산해야 할 수도 있기 때문



![Image.png](Stream%20Gatherers.assets/Image%20(4).png)

- push(element) 메소드를 가짐
    - 이 메소드의 반환값은 boolean 이며, 다운스트림이 더 이상 요소를 받기를 원하는지 여부를 나타냄
        - 이 정보를 통해 업스트림을 short-circuit(조기종료) 정보를 전파할 수 있음
- isRejecting()
    - 이 다운스트림에서 푸시 메서드 호출을 거부할지 여부를 나타내며, true 을 반환해도 후속 푸시가 성공한다는 보장은 없지만 false을 반환하면 다시는 true을 반환해서는 안 됩니다.



Gatherer 사용 사례

```java
List<List<String>> result = Stream.of("a", "b", "c", "d", "e")
                .gather(Gatherers.windowFixed(2))
                .toList();
        System.out.println(result); // [[a, b], [c, d], [e]]
        List<List<String>> result2 = Stream.of("a", "b", "c", "d", "e")
                .gather(Gatherers.windowSliding(2))
                .toList();
        System.out.println(result2); // [[a, b], [b, c], [c, d], [d, e]]

        List<String> result3 = Stream.of("a", "b", "c", "d", "e")
                .gather(Gatherers.scan(() -> "", (a, b) -> a + b))
                .toList();
        System.out.println(result3); // [a, ab, abc, abcd, abcde]

        List<String> result4 = Stream.of("a", "b", "c", "d", "e")
                .gather(Gatherers.fold(() -> "", (a, b) -> a + b))
                .toList();
        System.out.println(result4); // [abcde]

        List<String> result5 = Stream.of("a", "b", "c", "d", "e")
                .gather(Gatherers.mapConcurrent(2, String::toUpperCase))
                .toList();
        // maxConcurrency 값만큼 virtual Thread가 생성되어 병렬로 실행됨
        System.out.println(result5); // [A, B, C, D, E]
```

커스텀 구현 예

인덱스와 Value 를 같이 사용할 수 있는 Gatherer 구현

```java
public record IndexedValue<V>(int index, V value) {}

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

public class IndexedGatherer<V> implements Gatherer<V, AtomicInteger, IndexedValue<V>> {

    @Override
    public Supplier<AtomicInteger> initializer() {
        return () -> new AtomicInteger(0); // 초기 상태: 인덱스 0부터 시작
    }

    @Override
    public Integrator<AtomicInteger, V, IndexedValue<V>> integrator() {
        return (state, element, downstream) -> {
            int index = state.getAndIncrement(); // 현재 index 가져오고 +1
            downstream.push(new IndexedValue<>(index, element)); // 값 전달
            return true; // 계속 진행
        };
    }
}
```

사용 사례

```java
  List<IndexedValue<String>> result6 = Stream.of("a", "b", "c")
                .gather(new IndexedGatherer<>())
                .toList();
        result6.forEach(iv -> System.out.println(iv.index() + " => " + iv.value()));
        /*
         * 0 => a
         * 1 => b
         * 2 => c
         */
```