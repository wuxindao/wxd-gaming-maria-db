package wxdgaming.mariadb.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wxdgaming.mariadb.server.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class ConsoleMain {

    static Logger log = LoggerFactory.getLogger(ConsoleMain.class);

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            List<String> strings = GraalvmUtil.jarResources();
            for (String string : strings) {
                URL resource = contextClassLoader.getResource(string);
                System.out.println(string + " - " + resource);
            }
            ReflectAction reflectAction = ReflectAction.of();
            reflectAction.action(MyDB.class, false);
            reflectAction.action(DBFactory.class, false);
        }
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("my.ini")));
        WebService.getIns().setPort(Integer.parseInt(properties.getProperty("web-port")));
        DBFactory.getIns().init(
                properties.getProperty("database"),
                Integer.parseInt(properties.getProperty("port")),
                properties.getProperty("user"),
                properties.getProperty("pwd")
        );
        DBFactory.getIns().print();
        WebService.getIns().start();
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                log.info("敲回车，等待程序退出");
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        thread.start();

        final Thread hook = new Thread(() -> {
            DBFactory.getIns().stop();
            Runtime.getRuntime().halt(0);
        });
        Runtime.getRuntime().addShutdownHook(hook);

        if (args.length > 0) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30_000);
                    hook.start();
                } catch (Exception ignore) {}
            });
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        bufferedReader.readLine();
        try {
            thread.interrupt();
            thread.join();
        } catch (Exception ignore) {}

        /*增加30秒强制退出*/
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30_000);
                Runtime.getRuntime().halt(0);
            } catch (Exception ignore) {}
        });
        hook.run();
    }


}