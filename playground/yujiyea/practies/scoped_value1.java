import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;

public class scoped_value1 {
    final static ScopedValue<String> SCOPED_VALUE = ScopedValue.newInstance();

	public static void main(String[] args) {
		normalMethod();
	}

	private static void normalMethod() {
		ScopedValue.where(SCOPED_VALUE, "task-out1").run(()->{
            method();
            method2();
        });
        System.out.println("method() after scoped value isBound: " 
			+ SCOPED_VALUE.isBound());
	}

	private static void method() {
		long threadId = Thread.currentThread().threadId();
		System.out.println("method, threadID: " + threadId +
			", scoped value: " + SCOPED_VALUE.get());
		ScopedValue.where(SCOPED_VALUE, "submethod " + SCOPED_VALUE.get())
			.run(() -> subMethod(threadId));
		System.out.println("method end threadID: " + threadId +
			", scoped value: " + SCOPED_VALUE.get());
	}

    private static void method2() {
		long threadId = Thread.currentThread().threadId();
		System.out.println("method22222의 threadID: " + threadId +
			", scoped value: " + SCOPED_VALUE.get());
	}

	private static void subMethod(Long parentThreadId) {
		long threadId = Thread.currentThread().threadId();
		System.out.println("sub method, parent threadID: " + parentThreadId +
			", threadID: " + threadId + ", scoped value: " + SCOPED_VALUE.get());
	}
}
