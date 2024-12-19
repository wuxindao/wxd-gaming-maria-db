package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
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
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.WebService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
                /*必须让界面闪一下，不然程序不稳定，容易崩溃*/
                Thread.sleep(1000);
                Platform.runLater(() -> primaryStage.hide());
            } catch (InterruptedException ignore) {}
        });
        startDb(primaryStage, true);
    }

    public void startDb(Stage primaryStage, boolean checked) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                Properties properties = new Properties();
                properties.load(Files.newInputStream(Paths.get("my.ini")));
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
                BufferedImage bufferedImage = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(iconName));
                TrayIcon trayIcon = new TrayIcon(bufferedImage, title);
                trayIcon.setImageAutoSize(true);
                PopupMenu popup = new PopupMenu();
                {
                    MenuItem menuItem = new MenuItem("Open");
                    menuItem.addActionListener(e -> Platform.runLater(WebService.getIns().getShowWindow()));
                    popup.add(menuItem);
                }
                popup.add("-");
                {
                    MenuItem menuItem = new MenuItem("ClearDb");
                    menuItem.addActionListener(event -> {
                        DBFactory.getIns().stop();
                        WebService.getIns().stop();
                        try {
                            Files.walkFileTree(Paths.get("data-base"), new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                        throws IOException {
                                    Files.delete(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                                        throws IOException {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace(System.out);
                        }
                        startDb(primaryStage, false);
                    });
                    popup.add(menuItem);
                }
                popup.add("-");
                {
                    MenuItem menuItem = new MenuItem();
                    menuItem.setLabel("Close");
                    menuItem.addActionListener(event -> System.exit(0));
                    popup.add(menuItem);
                }

                /*TODO 图标双击事件 */
                trayIcon.addActionListener(event -> Platform.runLater(WebService.getIns().getShowWindow()));
                trayIcon.setPopupMenu(popup);
                tray.add(trayIcon);
            }
        } catch (Throwable e) {
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