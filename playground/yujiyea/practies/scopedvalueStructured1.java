import java.util.concurrent.StructuredTaskScope;

public class scopedvalueStructured1 {
    final static ScopedValue<String> SCOPED_VALUE = ScopedValue.newInstance();

	public static void main(String[] args) {
		ScopedValue.where(SCOPED_VALUE, "kim").run(() -> {

            System.out.println("부모 스레드 ID: " + Thread.currentThread().threadId() 
                + ", scoped value: " + SCOPED_VALUE.get());

            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                var childTask = scope.fork(() -> task());

                scope.join();
                scope.throwIfFailed();

                System.out.println("자식 작업 결과: " + childTask.get());
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
	}

    private static String task(){
        long currentThread = Thread.currentThread().threadId();
        System.out.println("자식 스레드 ID: " + currentThread + ", scoped value: " + SCOPED_VALUE.get());
        subTask(currentThread);

        return "Hello from child";
    }

    private static void subTask(Long parentThread){
        System.out.println("parent task 스레드 ID: "+ parentThread +", subTask 스레드 ID: " + Thread.currentThread().threadId() + ", scoped value: " + SCOPED_VALUE.get());
    }
}
