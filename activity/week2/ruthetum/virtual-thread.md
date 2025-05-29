# Virtual Thread
> https://www.youtube.com/watch?v=BZMZIM-n4C0
> 
> https://techblog.woowahan.com/15398/

너무 잘 정리된 글이 많당..

### 주요 키워드
- Thread 와 Virtual Thread 의 동작 구조
- Virtual Thread의 동작 원리
- Virtual Thread 사용 시 주의사항


### 같이 본 글 들
- https://velog.io/@appti/Virtual-Thread
  - 플랫폼 스레드랑 비교 및 VT 동작/구조
  - 영상이랑 내용 매우 유사

- https://0soo.tistory.com/259
  - 기본 개념 + 속성 예제
  - 시리즈로 이어지는 내용인데
    - 2편은 구조적 동시성: https://0soo.tistory.com/260
    - 3편은 SpringBoot 적용 예시: https://0soo.tistory.com/261

- https://tech.kakaopay.com/post/ro-spring-virtual-thread/
  - virtual thread 살짝 개념 + 간단한 성능 테스트
- https://tech.kakaopay.com/post/coroutine_virtual_thread_wayne/
  - 코루틴이랑 virtual thread 비교 + 성능 테스트

- https://d2.naver.com/helloworld/1203723
  - 조금 Low 한데 개념 정리 잘 되어있음

- https://waterfogsw.tistory.com/72
  - pinning은 24에서 해소함
    - synchronized (모니터 락) 블록에서 IO 또는 블로킹 작업 수행 시 가상 스레드가 캐리어(플랫폼) 스레드 붙잡음
    - 또는 스레드 상태 전환(wait) 시 발생
      - wait 호출 중에도 가상 스레드는 캐리어 스레드와 연결
      - 깨어난 후 다시 모니터를 재획득해야 하는데, 이 과정에서도 플랫폼 스레드가 고정
    - 그 외에 단순히 개발자가 코드를 잘못 써서가 아님. 라이브러리 때문인 경우도 있음 (MySQL Connector 등) 
  - 어케 해결함?
    - [JEP491](https://openjdk.org/jeps/491)의 특징 요약하면 아래와 같음
      - 모니터 소유권을 가상 스레드 기준으로 변경
        - 가상 스레드가 synchronized 블록 진입 시 JVM은 해당 가상 스레드를 모니터 소유자로 설정 (플랫폼 스레드와 독립적으로 모니터 소유 가능)
      - Object.wait()의 동작 개선
        - 가상 스레드는 플랫폼 스레드에서 Unmount
        - 대기 상태가 끝나면 JVM 스케줄러는 가상 스레드를 새로운 플랫폼 스레드에 Mount

- https://www.youtube.com/watch?v=MnkUX_E9SLg
  - https://springcamp.ksug.org/2024/static/docs/T2S1.pdf
  - 코루틴 반, virtual thread 반
    - 각각의 장단점 소개
  - 그리고 코루틴과 VT를 통합해서 사용하는 방법 제시
    - VT를 코루틴의 디스패처로 사용
  - 이로 인한 장점
    - 코루틴의 단점인 블로킹 상황에서의 성능 하락을 VT가 해결
    - 자바에서 계속해서 나오고는 있지만, 이미 코루틴에 잘 갖춰진 구조적 동시성과 코루틴 컨텍스트를 잘 활용할 수 있음
    - VT에서 아직 지원하지 않는 백프레셔 및 스트리밍 등을 코루틴 라이브러리를 통해 구현 가능

- 위에서 언급한 라이브러리의 이슈 - MySQL Connector
  - 이슈
    - https://bugs.mysql.com/bug.php?id=110512
  - https://medium.com/naukri-engineering/virtual-threads-and-mysql-unlocking-performance-gains-with-mysql-connector-j-9-0-0-6754abc85f61
    - 9.0.0 버전 오면서 해결된 듯
    - https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-0-0.html
    - https://github.com/mysql/mysql-connector-j/commit/00d43c5e8b24f1d516f93eea900b3487c15a489c
