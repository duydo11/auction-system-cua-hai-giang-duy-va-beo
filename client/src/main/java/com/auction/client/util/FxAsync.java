package com.auction.client.util;

import javafx.concurrent.Task;

import java.util.function.Consumer;

/**
 * Tiện ích chạy tác vụ nền cho JavaFX.
 *
 * <p>Dùng để chuyển các thao tác chờ máy chủ/CSDL ra khỏi JavaFX Application Thread,
 * sau đó trả kết quả về luồng giao diện qua hàm gọi lại.</p>
 */
public final class FxAsync {
    private FxAsync() {
    }

    /**
     * Overload tiện dụng: không cần truyền onError, lỗi sẽ in ra console.
     */
    public static <T> void run(String threadName, TaskAction<T> action,
                               Consumer<T> onSuccess) {
        run(threadName, action, onSuccess, Throwable::printStackTrace);
    }

    public static <T> void run(String threadName, TaskAction<T> action,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.execute();
            }
        };

        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));

        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    @FunctionalInterface
    public interface TaskAction<T> {
        T execute() throws Exception;
    }
}
