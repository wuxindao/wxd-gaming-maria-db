package wxdgaming.mariadb;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.LogbackResetTimeFilter;
import wxdgaming.mariadb.server.WebService;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
public class ApplicationMain {


    public static void main(String[] args) throws Exception {
        initGraalvm();
        DbConfig.loadYaml();
        GraalvmUtil.initGui();
        startDb(true);
        Thread.sleep(2000);
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

                reflectAction("com.sun.javafx");
                reflectAction("javafx");
                reflectAction("wxdgaming");
            }
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }
    }

    public static void reflectAction(String packageName) {
        ReflectAction reflectAction = ReflectAction.of();
        Reflections reflections = new Reflections(packageName);
        reflections.getSubTypesOf(Object.class).stream().parallel().forEach(clazz -> {
            try {
                reflectAction.action(clazz, false);
            } catch (Exception ignored) {}
        });
    }

    public static void startDb(boolean checked) {
        GraalvmUtil.write(1, "启动中");
        DbConfig.loadYaml();
        RunAsync.async(() -> {
            try {
                Thread.sleep(500);
                if (checked) {

                    try (HttpClient client = HttpClient.newHttpClient()) {

                        HttpRequest build = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + DbConfig.ins.getWebPort() + "/api/db/show")).build();
                        HttpResponse<byte[]> send = client.send(build, HttpResponse.BodyHandlers.ofByteArray());
                        /*正常访问说明已经打开过，退出当前程序，*/
                        System.exit(0);
                        return;
                    } catch (Exception ignore) {
                        /*如果访问报错说没有启动过；*/
                    }
                    System.out.println("可以正常开启");

                }

                boolean initResult = DBFactory.getIns().init(
                        DbConfig.ins.getDataBases(),
                        DbConfig.ins.getPort(),
                        DbConfig.ins.getUser(),
                        DbConfig.ins.getPwd()
                );

                if (!initResult) return;

                WebService.getIns().start(DbConfig.ins.getWebPort());
                WebService.getIns().initShow();
                DBFactory.getIns().print();

            } catch (Throwable e) {
                log.error("start failed ", e);
                System.out.println("启动异常了！");
                GraalvmUtil.write(99, "启动异常：" + e.toString());
                try {
                    /*如果异常弹出界面*/
                    WebService.getIns().getShowWindow().run();
                } catch (Exception ignore) {}
            }
        });
    }

}