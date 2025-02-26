package wxdgaming.mariadb.winfm;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import wxdgaming.mariadb.server.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ApplicationMain {

    public static Properties properties = null;
    public static CountDownLatch guiCountDownLatch = new CountDownLatch(1);

    public static String serverName() {
        return String.valueOf(properties.getOrDefault("title", "数据库服务"));
    }

    public static String javaClassPath() {
        return System.getProperty("java.class.path");
    }

    public static String readHtml() {
        try (InputStream resourceAsStream = ApplicationMain.class.getResourceAsStream("/consolebox.html")) {
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("加载文件失败");
        }
    }

    public static void main(String[] args) throws Exception {
        initGraalvm();
        loadProperties();

        Thread.ofPlatform().start(() -> ApplicationMain.initGui(guiCountDownLatch));
        guiCountDownLatch.await();
        startDb(true);

        Thread.sleep(2000);
    }

    public static void loadProperties() {
        try {
            properties = new Properties();
            try (InputStream inputStream = Files.newInputStream(Paths.get("my.ini"));
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(inputStreamReader);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
    }

    public static void initGui(CountDownLatch countDownLatch) {
        try {
            Application.launch(DbApplication.class);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        } finally {
            countDownLatch.countDown();
        }
    }

    public static void initGraalvm() {
        try {
            LogbackResetTimeFilter.out = true;
            if (System.getProperty("build.graalvm") != null) {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                List<String> strings = GraalvmUtil.jarResources();
                for (String string : strings) {
                    URL resource = contextClassLoader.getResource(string);
                    log.info("{} - {}", string, resource);
                }
                ReflectAction reflectAction = ReflectAction.of();
                {
                    Reflections reflections = new Reflections("javafx.reflections");
                    Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
                    for (Class<?> clazz : allClasses) {
                        try {
                            reflectAction.action(clazz, false);
                        } catch (Exception ignored) {}
                    }
                }
                {
                    Reflections reflections = new Reflections("wxdgaming");
                    Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
                    for (Class<?> clazz : allClasses) {
                        try {
                            reflectAction.action(clazz, false);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }
    }

    public static void startDb(boolean checked) {
        DBFactory.write(1, "启动中");
        loadProperties();
        RunAsync.async(() -> {
            try {
                Thread.sleep(500);
                if (checked) {
                    int webPort = Integer.parseInt(properties.getProperty("web-port"));

                    try (HttpClient client = HttpClient.newHttpClient()) {

                        HttpRequest build = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + webPort + "/api/db/show")).build();
                        HttpResponse<byte[]> send = client.send(build, HttpResponse.BodyHandlers.ofByteArray());
                        /*正常访问说明已经打开过，退出当前程序，*/
                        System.exit(0);
                        return;
                    } catch (Exception ignore) {
                        /*如果访问报错说没有启动过；*/
                    }
                    System.out.println("可以正常开启");

                    WebService.getIns().setPort(webPort);
                }

                boolean initResult = DBFactory.getIns().init(
                        properties.getProperty("database"),
                        Integer.parseInt(properties.getProperty("port")),
                        properties.getProperty("user"),
                        properties.getProperty("pwd")
                );

                if (!initResult) return;

                WebService.getIns().start();
                WebService.getIns().initShow();
                DBFactory.getIns().print();

            } catch (Throwable e) {
                log.error("start failed ", e);
                System.out.println("启动异常了！");
                DBFactory.write(99, "启动异常：" + e.toString());
                try {
                    /*如果异常弹出界面*/
                    WebService.getIns().getShowWindow().run();
                } catch (Exception ignore) {}
            }
        });
    }

}