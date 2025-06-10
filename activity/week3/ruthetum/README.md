# 카카오페이는 어떻게 수천만 결제를 처리할까? 우아한 결제 분산락 노하우 / if(kakaoAI)2024
> https://www.youtube.com/watch?v=4wGTavSyLxE

### 요약
- AOP 기반으로 분산락 처리 시 비효율적(임계영역 외 추가로 락을 잡음)이거나 유지보수 시 휴먼에러가 발생할 수 있는 소지가 있다
- 따라서 이를 함수형(데코레이션 함수)으로 변경해서 효율을 높이고 유지보수성을 높임

#### 분산 락 관련
- 분산 락 활용 시 redission을 사용함
  - vs Lettuce
- 구현 시 용이함과 Lock 여부 확인 시 부하를 줄일 수 있다는 장점때문에 일반적으로 redission 사용했다
  - 용이함: redission은 락 관련 인터페이스가 잘 정의되어 있어 사용하기 편리함
  - 부하 감소: redission은 락 획득/해제를 pub/sub 방식으로 처리 (vs spin lock)