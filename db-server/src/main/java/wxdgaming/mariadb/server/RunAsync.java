package wxdgaming.mariadb.server;

/**
 * 异步执行
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-12 09:27
 **/
public class RunAsync {

    public static void async(Runnable runnable) {
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                e.printStackTrace(System.out);
            }
        }).start();
    }

}
