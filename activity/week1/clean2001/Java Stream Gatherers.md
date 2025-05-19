# Java Stream Gatherers

아래 영상을 보고 정리한 내용입니다.

https://www.youtube.com/watch?v=v_5SKpfkI2U

# 영상 요약

스트림 수집기(Stream Gatherers)는 Java 22부터 도입된 새로운 기능으로, **스트림 처리 시 필요한 다양한 중간 연산을 개발자가 직접 구현할 수 있게 해줍니다. (Java 24에서 final)**

기존의 스트림 API에서 제공하는 중간 연산은 한정적이었지만, **스트림 수집기를 통해 개발자가 자신만의 맞춤형 중간 연산을 정의할 수 있게 되었다.** 이를 통해 더 다양한 스트림 처리 패턴을 구현할 수 있게 되었다.

- 스트림 수집기는 Java22부터 도입된 새로운 기능으로, 개발자가 자신만의 중간 연산을 정의할 수 있게 해줌.
- 기존 스트림 API의 중간 연산은 한정적이었지만, Gatherer를 통해 더 다양한 스트림 처리 패턴을 구현할 수 있게 됨
- 스트림 수집기는 **initializer, integrator, combiner, finisher** 등을 통해 중간 연산을 구현할 수 있게 해줌
- stream gatherer는 순차처리, 병렬처리를 모두 지원하며, 상태 유지 여부, 입출력 비율 등 다양한 요구 사항을 충족할 수 있다.
- Java 표준 라이브러리에 포함된 다양한 Stream Gatherer함수(fold, scan, window 등)를 활용할 수 있음

---

# 영상 내용 정리

![스크린샷 2025-05-19 오후 3.21.11.png](../.asset/clean2001/week1-2/3.21.11.png)

강의자는 이걸 다 묶어서 파이프라인이라고 부르겠다고 함.

다른 중간 작업을 할 수 있다면? 뭘 할 것임?

filter, map 같은 기존 내장 기능 말고도 다른 걸 할 수 있다면 뭘 할것임?

![스크린샷 2025-05-19 오후 3.26.27.png](../.asset/clean2001/week1-2/3.26.27.png)

![스크린샷 2025-05-19 오후 3.26.41.png](../.asset/clean2001/week1-2/3.26.41.png)

이런 급수 연산(?)(=scan operation) 같은걸 할려면?

⇒ 이런 연산들을 하려면 사물을 기억할 수 있는 상태 저장 중간 작업이 필요하다.

그럼 `Stream::collect(Collector)`로는 충분하지 않은 경우 있음 

## Collector

![스크린샷 2025-05-19 오후 3.40.37.png](../.asset/clean2001/week1-2/3.40.37.png)

T: Collector에 대한 입력 요소의 유형

A: 유형 또는 상태

R: 결과 값

### Supplier<A> supplier()

Provides the state, if any, to be used during evaluation of the Collector.

사용할 수 있는 상태를 제공?

### BiConsumer<A, T> accumulator()

Each input element is applied to the accumulator, together with the state

### BinaryOperator<A> combiner()

When parallelized this is used to merge partial results into one.

병렬 연산 시, 부분 결과를 하나로 통합하는 데 사용

![스크린샷 2025-05-19 오후 3.46.20.png](../.asset/clean2001/week1-2/3.46.20.png)

Collector는 n개의 요소를 사용해서 하나의 결과 요소를 생성한다.

이외에 다른 모드는 지원하지 않음

무한한 스트림을 지원하지 않는다.(?)

(⇒ 이유를 이해하지 못함)

---

## Stream Gatherers

![스크린샷 2025-05-19 오후 4.14.19.png](../.asset/clean2001/week1-2/4.14.19.png)

java.util.stream.Gatherer<T, A, R>

- Supplier<A> initializer()
    - stream 인스턴스가 사용할 `A` 타입의 누적기(state)를 생성
- Integrator<A, T, R> integrator()
    - 입력된 `T` 를 받아서 `A` 에 저장하고, 필요시에 `R` 을 downstream에 `push()`
- BinarayOperator<A> combiner()
    - 병렬 스트림 처리시에 두 state를 병합
- BiConsumer<A, Downstream<R>> finisher()
    - 스트림 종료시, state 안에 있는 데이터를 push

![스크린샷 2025-05-19 오후 4.19.02.png](../.asset/clean2001/week1-2/4.19.02.png)

![스크린샷 2025-05-19 오후 4.27.14.png](../.asset/clean2001/week1-2/4.27.14.png)

push라는 단일 추상 메서드가 있으며 요소를 취하고 boolean을 반환한다.

return boolean 부분은 매우 중요한데, 그 반환값은 Downstream이 더 많은 요소를 받기 원하는지 여부이다.

---

## Gatherer 실습

### ✨ map

1:1, stateless, parallelizable 연산

하나 넣으면 하나 나옴.

어떤 상태도 가지고 있지 않기 때문에, 병렬화 가능

```java
Stream.of(1, 2, 3, 4)
		.gather(map(i -> i+1))
		.toList();
		
// 결과: [1, 2, 3, 4]
```

### ✨ mapMulti

1:N, stateless, parallelizable 연산

```java
Stream.of(List.of(1, 2), List.of(3, 4))
		.gather(mapMulti(List::forEach))
		.toList();
	
```

두 목록의 각 요소에 대해 해당 값에 대한 Consumer 호출

mapMulti: flatMap과 유사하게 평탄화 하는 연산이지만, 불필요한 중간 객체(Stream, List 등)을 만들지 않고 직접 요소를 push 하기 때문에 성능적으로 더 효율적

### ✨ limit

1:0, 1:0, stateful, sequential 연산

![스크린샷 2025-05-19 오후 5.46.35.png](../.asset/clean2001/week1-2/5.46.35.png)

---

## Sequential과 Parallel 연산

## sequential evaluation

```java
<T, A, R> void evaluate(Interator<T> source, Gaherer<T, A, R> gatherer, Consumer<R> output) {
	Gatherer.Downstream<R> downstream = (R r) -> {output.accept(r); return true;};
	var state = gatherer.initializer().get();
	var integrator = gatherer.integrator();
	
	while(source.hasNext() && integrate(state, source.next(), downstream)) {}
	gatherer.finisher().accept(state, downstream);   

}
```

## parallel evaluation

모든 요소를 병렬적으로 더하는 예시를 통해 알아보자.

(1) 정보 소스가 있고, 이를 병렬화하려면 소스를 분할해야한다.

작업에 적합하다고 생각되는 충분히 작은 청크가 나올때까지 업스트림 데이터(소스데이터)를 계속 재귀적으로 분할한다.

![스크린샷 2025-05-19 오후 4.51.59.png](../.asset/clean2001/week1-2/4.51.59.png)

(2) 그런 다음, 각 leaf(말단)마다 Gatherer.Downstream을 만든다.

(3) 그 다음, 각각의 상태에 대해 gatherer.initializer().get()한다.

(4) leaf에서 이런 작은 순차적인 evaluation을 만들고, 작은 병렬화된 순차 응용 프로그램을 나란히 실행한다.

![스크린샷 2025-05-19 오후 4.52.20.png](../.asset/clean2001/week1-2/4.52.20.png)

(5) 부분적인 결과들을 병합(root에 다다를 때까지)

![스크린샷 2025-05-19 오후 4.52.29.png](../.asset/clean2001/week1-2/4.52.29.png)

재귀적으로 실행

(6) 마지막에 downstream의 finisher 호출

![스크린샷 2025-05-19 오후 4.53.37.png](../.asset/clean2001/week1-2/4.53.37.png)

![스크린샷 2025-05-19 오후 4.53.44.png](../.asset/clean2001/week1-2/4.53.44.png)

takeWhile 예시(27분 30초 경) 

만약, 오른쪽 경우에서 curcuit signal이 있는경우?

![스크린샷 2025-05-19 오후 4.57.17.png](../.asset/clean2001/week1-2/4.57.17.png)

만약 무한 스트림을 재귀적으로 분할하고 스트림의 순서의 어느 지점에서 완료되는 경우, 오른쪽에서 무한대를 계속 처리하면 안됨 ⇒ 무슨 말이지(이해 완전히 안됨) 

청크 뒤에 있는 것들을 처리할 모든 작업을 취소한다.

반드시 처리할 필요 없이 버릴 수 있음

---

## Gatherer 내장 연산

### Gatherers.fold()

stateful, sequential, N:0, N:1 연산

현재 상태를 다음 요소와 결합하여 새로운 상태 리턴

![스크린샷 2025-05-19 오후 5.01.47.png](../.asset/clean2001/week1-2/5.01.47.png)

reduce와 유사

### Gatherers.scan()

staeful, sequential, 1:1 연산

fold와 유사하지만 모든 중간 결과를 반환한다.

![스크린샷 2025-05-19 오후 5.03.07.png](../.asset/clean2001/week1-2/5.03.07.png)

### Gatherers.windowFixed()

stateful, sequential, N:M operation

![스크린샷 2025-05-19 오후 5.05.16.png](../.asset/clean2001/week1-2/5.05.16.png)

마지막에 window 사이즈보다 요소가 적게 남아있다면, 마지막 element는 윈도우 사이즈보다 작을 수 있음

### Gatherers.mapConcurrent()

stateful, sequential, 1:1 연산

Stream을 병렬로 처리하면서 동시(concurrent)에 중간 연산을 수행할 수 있는 메서드이다.

![스크린샷 2025-05-19 오후 5.07.37.png](../.asset/clean2001/week1-2/5.07.37.png)

---

## Connection Gatherers

문자열을 취하고 선택적 정수를 생성하는 gatherer가 있다고 가정해보자. (parsing gatherer)

그리고 empty를 버리는 gatherer가 있다고 해보자(dropEmptyInts)

![스크린샷 2025-05-19 오후 5.15.36.png](../.asset/clean2001/week1-2/5.15.36.png)

---

# 영상 내용 외 더 찾아본 정보

## 예시 코드

5개씩 리스트로 묶어 출력하는 예제이다.

```java
Stream<Integer> stream = IntStream.rangeClosed(1, 12).boxed();

Stream<List<Integer>> result = stream.gather(
    Gatherer.ofSequential(
        () -> new ArrayList<Integer>(),                          // initializer: 초기 state
        (list, item, downstream) -> {
            list.add(item);
            if (list.size() == 5) {
                downstream.push(List.copyOf(list));              // integrator: 5개마다 방출
                list.clear();
            }
            return true;
        },
        (list, downstream) -> {
            if (!list.isEmpty()) {
                downstream.push(List.copyOf(list));              // finisher: 마지막 남은 요소 처리
            }
        }
    )
);

```

출력:

```java
[1, 2, 3, 4, 5]
[6, 7, 8, 9, 10]
[11, 12]
```

## ofSequential 메서드

gatherer를 sequential하게 처리함을 보장

![스크린샷 2025-05-19 오후 5.40.43.png](../.asset/clean2001/week1-2/5.40.43.png)