package wxdgaming.mariadb.server;

import com.sun.javafx.application.PlatformImpl;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 异步执行
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-12 09:27
 **/
public class RunAsync {

    private static final LinkedBlockingQueue<PlatformRunnable> uiRunnableQueue = new LinkedBlockingQueue<>();

    static {
        Thread thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    PlatformRunnable take = uiRunnableQueue.take();
                    PlatformImpl.runAndWait(() -> {
                        try {
                            take.run();
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    });
                } catch (Throwable ignored) {}
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /** 执行ui更新事件 */
    public static void runUI(PlatformRunnable runnable) {
        uiRunnableQueue.add(runnable);
    }

    public static void async(PlatformRunnable runnable) {
        Thread thread = new Thread(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                e.printStackTrace(System.out);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

}
