package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.WebService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class HelloApplication extends Application {

    final String title = "wxd-gaming-数据库服务";
    final String iconName = "db-icon.png";

    @Override
    public void start(Stage primaryStage) throws Exception {

        setIcon(primaryStage);

        Image image_logo = new Image(iconName);
        /*阻止停止运行*/
        Platform.setImplicitExit(false);
        WebService.getIns().setShowWindow(() -> {
            PlatformImpl.runAndWait(() -> {
                /*todo 必须要交给ui线程*/
                primaryStage.setIconified(false);
                primaryStage.show();
                /*设置最顶层*/
                primaryStage.setAlwaysOnTop(true);
                primaryStage.setAlwaysOnTop(false);
            });
        });
        Class<HelloApplication> helloApplicationClass = HelloApplication.class;
        URL resource = helloApplicationClass.getResource("hello-view.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(resource);

        Scene scene = new Scene(fxmlLoader.load(), Color.BLACK);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);

        primaryStage.getIcons().add(image_logo);
        // primaryStage.initStyle(StageStyle.UNDECORATED);    // 可以隐藏任务栏上的图标
        primaryStage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            select(primaryStage);
        });
        /*todo 通过调用 http://localhost:19902/api/db/show */
        primaryStage.show();
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setAlwaysOnTop(false);
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                Properties properties = new Properties();
                properties.load(Files.newInputStream(Paths.get("my.ini")));
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
                DBFactory.getIns().init(
                        properties.getProperty("database"),
                        Integer.parseInt(properties.getProperty("port")),
                        properties.getProperty("user"),
                        properties.getProperty("pwd")
                );
                DBFactory.getIns().print();
                WebService.getIns().start();
                WebService.getIns().initShow();

                CompletableFuture.runAsync(() -> {

                    try {
                        Thread.sleep(10_000);
                        PlatformImpl.runAndWait(() -> {
                            primaryStage.hide();
                        });
                    } catch (Exception ignore) {}

                });
            } catch (Throwable e) {
                e.printStackTrace(System.out);
                System.out.println("启动异常了！");
                System.out.println("启动异常了！");
                System.out.println("启动异常了！");
                System.out.println("启动异常了！");
            }
        });
    }

    /** 开启系统托盘图标 */
    public void setIcon(Stage primaryStage) {
        try {
            if (SystemTray.isSupported()) {
                /*TODO 系统托盘图标*/
                SystemTray tray = SystemTray.getSystemTray();
                PopupMenu popup = new PopupMenu();

                // MenuItem menuItem = new MenuItem();
                // menuItem.setLabel("Exit");
                // menuItem.addActionListener(new ActionListener() {
                //     public void actionPerformed(ActionEvent e) {
                //         System.exit(0);
                //     }
                // });
                // popup.add(menuItem);

                BufferedImage bufferedImage = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(iconName));
                TrayIcon trayIcon = new TrayIcon(bufferedImage, title, popup);
                trayIcon.setImageAutoSize(true);
                trayIcon.addActionListener(new ActionListener() {
                    /*TODO 图标双击事件 */
                    @Override public void actionPerformed(ActionEvent e) {
                        WebService.getIns().getShowWindow().run();
                    }
                });
                tray.add(trayIcon);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void select(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("提示");
        alert.setHeaderText("点击按钮以确认你的选择!");
        // alert.setContentText("点击按钮以确认你的选择。");

        ButtonType buttonTypeOk = new ButtonType("关闭进程", ButtonBar.ButtonData.YES);
        ButtonType buttonTypeCancel = new ButtonType("后台运行", ButtonBar.ButtonData.OK_DONE);

        alert.getButtonTypes().setAll(buttonTypeCancel, buttonTypeOk);

        ButtonType result = alert.showAndWait().orElse(buttonTypeCancel);

        if (result == buttonTypeOk) {
            Runtime.getRuntime().exit(0);
        } else {
            primaryStage.hide();
        }
    }
}