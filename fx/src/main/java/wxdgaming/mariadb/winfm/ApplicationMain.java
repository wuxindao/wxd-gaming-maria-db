package wxdgaming.mariadb.winfm;

import javafx.application.Application;
import wxdgaming.mariadb.server.*;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApplicationMain {

    public static String javaClassPath() {
        return System.getProperty("java.class.path");
    }

    public static void main(String[] args) throws Exception {
        LogbackResetTimeFilter.out = true;
        if (javaClassPath() != null && javaClassPath().contains(".jar")) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            List<String> strings = GraalvmUtil.jarResources();
            for (String string : strings) {
                URL resource = contextClassLoader.getResource(string);
                System.out.println(string + " - " + resource);
            }

            ReflectAction reflectAction = ReflectAction.of();
            reflectAction.action(MyDB.class, false);
            reflectAction.action(DBFactory.class, false);
            reflectAction.action(HelloApplication.class, false);
            reflectAction.action(HelloController.class, false);

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100_000);
                    DBFactory.getIns().stop();
                    Runtime.getRuntime().halt(0);
                } catch (Exception ignore) {}
            });
        }

        Application.launch(HelloApplication.class);

    }
}