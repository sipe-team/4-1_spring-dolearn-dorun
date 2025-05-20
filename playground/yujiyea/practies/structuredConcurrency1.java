import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class structuredConcurrency1 {
    public static void main(String[] args) throws Exception{
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            Subtask<String> userTask = scope.fork(() -> fetchUserData());
            Subtask<String> orderTask = scope.fork(() -> fetchOrderData());

            scope.join();               // 모든 작업 종료 대기
            scope.throwIfFailed();      // 실패한 작업이 있으면 예외 발생

            String userData = userTask.get();
            String orderData = orderTask.get();

            System.out.println("User: " + userData);
            System.out.println("Order: " + orderData);
        }
    }

    static String fetchUserData() throws InterruptedException {
        Thread.sleep(1000);
        return "User123";
    }

    static String fetchOrderData() throws InterruptedException {
        Thread.sleep(1500);
        return "Order456";
    }
}
