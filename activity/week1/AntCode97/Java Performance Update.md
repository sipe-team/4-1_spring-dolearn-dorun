# Java Performance Update

최근 자바 23, 24에서 여러가지 성능 개선이 있었음

# JDK-8318446: C2 - "MergeStores"

![Image.png](Java%20Performance%20Update.assets/Image.png)

기존의 Java 에서 long 타입의 값을 byte 배열에 저장하려면, 위 사진의 코드처럼 long 의 값을 오른쪽으로 1바이트씩 Shift 시켜서 꺼낸다음 Byte 배열에 저장해야했음

→ 이 과정이 굉장히 번거롭고, 낭비가 심함

![Image.png](Java%20Performance%20Update.assets/Image%20(2).png)

이를 해결하려면 원래는 VarHandle 이나 Unsage 를 사용해서 BALE 라이브러리 코드를 사용해야함

VarHandle 과 Unsage : **Java 메모리 접근과 동기화**에 관련된 고급 기능을 제공하는 API

Varhandle 

- Java 9에서 도입된 API로, **변수에 대한 낮은 수준의 접근 및 원자적 연산**을 가능하게 함
- 변수에 대한 low-level 접근 (예: get, set, compareAndSet, getAndAdd 등)
- **메모리 배리어(visibility guarantees)** 제공
- 리플렉션처럼 필드 접근 가능하지만, **성능은 더 우수함**
- MethodHandle과 유사한 방식으로 동작

```java
class Example {
    volatile int x = 0;

    private static final VarHandle X_HANDLE;

    static {
        try {
            X_HANDLE = MethodHandles.lookup()
                .in(Example.class)
                .findVarHandle(Example.class, "x", int.class);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    void doSomething() {
        X_HANDLE.setVolatile(this, 42);
        int value = (int) X_HANDLE.getVolatile(this);
    }
}
```

Unsafe

- sun.misc.Unsafe는 JDK 내부 클래스이며, **정상적인 Java 보안 모델을 우회하여** 메모리 및 동기화 제어를 직접 할 수 있는 **비공식 저수준 API**
- 직접 메모리 접근 (allocateMemory, putInt, getLong 등)
- 객체 생성 우회 (allocateInstance)
- CAS 및 Fence 지원
- 메모리 배리어 제공



C2 컴파일러의 MergeStores 최적화는 위 코드 같이 long 값을 바이트 배열에 바이트 단위로 순차적으로 저장하는 패턴을 발견하면, long 값 하나를 통째로 저장하려는구나! 하고 눈치채고, 위 여러 작업을 하나의 기계어 명령어로 바꿈 → 즉 long 값 전체 8 바이트를 배열의 특정 위치에 한 번에 넣는 명령어를 만들어줌

오래된 자바 클래스인 RandomAccessFile 은 이 최적화를 통해 성능이 8배나 개선됨

하지만 aliasing 문제가 없는 경우에만 사용할 수 있음

aliasing : 서로 다른 코드 부분이 같은 메모리 위치를 가리키거나 건드는 상황 → 이런 상황이 발생할 가능성이 있다면 컴파일러는 최적화를 포기하고 원래대로 돌아감

예를 들어 "MergeStores"의 경우, 만약 바이트 배열에 값을 저장할 때 사용하는 "오프셋(offset, 배열 내의 위치)"이 코드 안에서 복잡하게 또는 동적으로 계속 변경된다면, C2는 "이게 같은 위치를 건드리는 건가? 다른 위치인가?" 하고 확신할 수 없게 됨. 이 경우에는 최적화를 진행하지 않음

그래서 "MergeStores" 최적화의 혜택을 받으려면 long 값을 쓸 때 사용하는 오프셋을 코드 내에서 정적으로 사용하고, 모든 저장 작업을 마친후에 그 다음을 위해 오프셋을 증가시키는 방식을 사용해야함



아직 MergeLoad 는 없음 : 값을 읽어오는 동작에 대한 최적화는 개발자가 VarHandle 같은 도구를 사용해야함



# JDK-8340821: FFM API Bulk Operations

![Image.png](Java%20Performance%20Update.assets/Image%20(3).png)

FFM API (Foreign Function & Memory API) 

- 자바가 자바 바깥 세상(JVM 힙 메모리 영역 바깥의 메모리나 운영체제 코드)과 더 쉽게 대화하고 데이터를 주고받을 수 있게 해주는 새로운 기능
- 자바 프로그램은 보통 JVM 안에서 실행되고 사용하는 메모리는 JVM이 관리함. 하지만 때때로 운영체제 레벨의 기능이나 C/C++ 로 작성된 기존의 빠른 라이브러리를 사용해야할 때가 있음
    - 이럴 때 FFM API 를 사용하면 자바 코드로 편하게 외부 함수를 호출하거나 JVM 이 관리하지 않는 메모리 영역에 접근해서 데이터를 읽고 쓸 수 있게 됨

- MemorySegment
    - FFM API 에서 메모리 영역을 표시하는 방법
    - 일반 자바 배열은 크기 제한이 있음 (약 2GB). 하지만 FFM API 의 MemorySegment 는 64비트 주소를 사용해서 그보다 훨씬 큰 메모리 영역을 다를 수있음
    - 이 메모리 영역은 JVM 힙 안(On-Heap)에 있을 수도 있고, 힙 바깥(Off-heap) 에 있을 수도 있음
- FFM API Bulk Operations
    - fill : 특정 값으로 메모리 영역 전체를 채울 수 있음
        - MemorySegment 를 할당할때마다, 보안의 이유로 자동으로 0으로 채워짐
    - copy : 한 MemorySegment 에서 다른 MemorySegment 로 데이터를 복사할 수 있음
    - mismatch : 두 MemorySegment 를 비교하여 처음으로 다른 위치를 찾음
- Internally backed by Unsafe
    - Bulk Operations 는 처음에 Unsafe 나 네이티브 코드를 이용해서 구현되었으나, 작은 크기의 메모리 영역에 대한 Bulk Operations 의 경우, 네이티브 코드를 호출하는 대신 Java 코드로 다시 구현했더니 훨씬 더 빨라짐
        - 자바에서 네이티브 코드를 호출할 때 전환 비용이 발생하는데, 작은 크기의 작업에서는 실제 처리시간보다 전환비용이 차지하는 비중이 커서 전체적으로 느려짐
        - 이런 발견덕분에 임계값(threshold) 를 두고, 작업크기가 threshold 보다 작으면 자바 코드로 구현된 빠른 로직을 사용



# JDK-8180450: Secondary Super Cache Scaling

![Image.png](Java%20Performance%20Update.assets/Image%20(4).png)

- instanceof 의 성능이 올라감
    - 기존에는 instanceof 를 처리하기 위해 one-element cache 를 사용했음
        - 최근에 검사했던 특정 슈퍼타입 정보를 캐시에 저장해두고, 다음에 동일한 검사가 오면 캐시에서 바로 결과를 가져와서 처리하는 방심
    - one-element cache 를 사용해왔음
        - Foo->Bar->Bazz 와 같은 상황에서, Foo instanceof Bar 는 true 이고 one-element cache 에 Bar 가 저장됨. Foo instanceof Bazz 하면, 캐시 정보에는 Bar 정보만 있으므로 캐시를 사용할 수 없고 클래스 계층을 찾아 Bazz 정보를 알아낸뒤 true 를 리턴. 그리고 기존 캐시정보를 덮어씀
        - one-element cache 방식은 다음 문제들을 유발함
            - Flip-flop situation 
                - 여러 스레드가 동시에 다른 슈퍼타입에 대한 instanceof 검사를 수행할 경우, 1개짜리 캐시의 내용이 계속해서 바뀌게 됨
            - NUMA 환경에서 심각함
                - NUMA(Non-Uniform Memory Access) 아키텍처 환경에서 다른 메모리 영역에 있는 스레드들이 이 캐시에 접근할 때 이러한 문제가 두드러짐
    - Immutable Lookup Table 를 사용하도록 개선함
        - 이전의 1개짜리 캐시보다 직접 접근 시에는 약간 더 느릴 수 있음
        - 하지만 캐시 내용이 변하지 않으므로 훨씬 더 예측 가능한 동작을 보여줌
        - 다중 스레드 어플리케이션에서 훨씬 효율적임. 스레드 간에 캐시 내용을 덮어쓰기 위해 경쟁할 필요가 없어지기 때문
    - C2 컴파일러에서 적용되어, 아직 C1 컴팔라이는 이전 방식을 사용함

# JDK-8336856: String Concatenation

![Image.png](Java%20Performance%20Update.assets/Image%20(5).png)

- "Hello, " + name + "!" 같은 문자열 연결 연산의 성능이 개선됨
- StartUp 성능을 개선하기 위해서 진행됨
- 이전에는 MethodHandle combinators 를 활용하였음
    - 일종의 레고 블록처럼 여러 메서드 핸들을 조합하여 문자열 연결 로직을 구성하는 방식
    - 메서드 핸들을 조합하는데 시간이 걸렸고, 이는 특히 어플리케이션 start up  성능에 영향끼쳤음
- Hidden Classes 를 직접 생성하는 방식으로 변경됨
    - 문자열 연결 로직을 담고 있는 Hidden Class 를 생성함
    - 이렇게 생성한 Hidden Class 는 자바 내부에 더 효율적으로 접근할 수 있음
    - 또한 재사용 가능함
    - String Concatenation 코드 생성시간을 40% 단축함
    - 코드 생성량 50% 감소
        - 중간 단계의 메소드 핸들 조합 과정이 사라지면서 코드 생성량 자체가 50% 감소했음
    - Throughput 유지
        - 이미 C2 컴파일러 등에 의해 최적화 되어 잇던 실제 문자열 연결 실행 시간은 동일하게 유지됨
        - 즉 실행중 성능 저하 없이 시작 성능만 개선됨

# JEP 474: ZGC: Generational Mode by Default

![Image.png](Java%20Performance%20Update.assets/Image%20(6).png)



ZGC 의 기본 기능 

- 매우 큰 Heap 을 지원하며 테라바이트 단위의 힙에서도 Stop The World 시간을 짧게 유지하는 것을 목표로함
- ZGC 자체는 JDK 15에 도입됨
- JDK 21 에서 Generational Mode ZGC 가 도입되었지만 기본적으로 활성화되어 있지는 않았음
    - JDK24 부터는 기본으로 활성화됨
    - 향후 Generational Mode 가 아닌 기존 ZGC 는 제거될 예정

Generational ZGC 의 개념

- 일반적으로 힙에 오래 머물러 있는 객체일수록 앞으로도 계속 사용될 확률이 높음
    - 대부분의 객체의 생명주기가 짧기 때문
    - 이러한 특성을 활용하기 위해, Generational ZGC 는 힙을 여러 세대로 나누고, 새로운 객체들을 더 자주 검사하고 오래된 객체들은 덜 자주 검사함



# JEP 450: Compact Object Headers (Experimental)

![Image.png](Java%20Performance%20Update.assets/Image%20(7).png)

- 객체 헤더 크기를 줄임
- 모든 자바 객체는 개발자가 직접 볼 수 없는 내부적인 헤더가 있음
    - 헤더는 객체의 클래스를 설명하고, 해시코드(System.identityHashCode) 같은 것은 JVM 이 객체 주소를 이동시킬 수 있기 때문에 헤더에 기록하여 동일하게 유지해야함. 또한 가비지 컬렉터가 사용하는 정보, 그리고 객체 동기화에 필요한 정보를 포함하고 있음
- 기존 객체 헤더의 크기를 96비트에서 64비트로 줄이는 것을 목표로함
    - 메모리 사용량이 10% 이상 줄어들 수 있음
    - 캐시 효율성이 향상됨
    - 