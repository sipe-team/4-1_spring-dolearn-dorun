import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class scopedvalueStructured {
    static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

    public static void main(String[] args) {
        String requestId = "req-12345";
        String userId = "user-6789";

        // 요청 처리
        String response = ScopedValue.where(REQUEST_ID, requestId)
            .where(USER_ID, userId)
            .run(()->processRequest());

        System.out.println(response);
    }

    public static String processRequest() {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Subtask<String> profileFuture = scope.fork(() -> fetchUserProfile());
            Subtask<String> ordersFuture = scope.fork(() -> fetchUserOrders());
            Subtask<String> notificationsFuture = scope.fork(() -> fetchUserNotifications());

            // 모든 작업 종료 대기
            scope.join();
            scope.throwIfFailed();

            // 결과 수집
            String profile = profileFuture.get();
            String orders = ordersFuture.get();
            String notifications = notificationsFuture.get();
            
            String response = "RequestID: " + REQUEST_ID.get() + "\n"
                    + "UserID: " + USER_ID.get() + "\n"
                    + "Profile: " + profile + "\n"
                    + "Orders: " + orders + "\n"
                    + "Notifications: " + notifications;

            // 최종 응답 생성
            return response;

        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    static String fetchUserProfile() throws InterruptedException {
        // Scoped Value로 현재 컨텍스트 확인 가능
        System.out.println("fetchUserProfile - RequestID: " + REQUEST_ID.get() + ", UserID: " + USER_ID.get());
        Thread.sleep(1000);
        return "User Profile Data";
    }

    static String fetchUserOrders() throws InterruptedException {
        System.out.println("fetchUserOrders - RequestID: " + REQUEST_ID.get() + ", UserID: " + USER_ID.get());
        Thread.sleep(1500);
        return "User Orders Data";
    }

    static String fetchUserNotifications() throws InterruptedException {
        System.out.println("fetchUserNotifications - RequestID: " + REQUEST_ID.get() + ", UserID: " + USER_ID.get());
        Thread.sleep(500);
        return "User Notifications Data";
    }
}
