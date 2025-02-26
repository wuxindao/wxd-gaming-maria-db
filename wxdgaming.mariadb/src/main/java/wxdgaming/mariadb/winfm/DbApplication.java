package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import wxdgaming.mariadb.ApplicationMain;
import wxdgaming.mariadb.DbConfig;
import wxdgaming.mariadb.GraalvmUtil;
import wxdgaming.mariadb.RunAsync;
import wxdgaming.mariadb.server.WebService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DbApplication extends Application {

    public static String __iconName = "db-icon.png";

    static AtomicBoolean icon_checked = new AtomicBoolean();


    @Override
    public void start(Stage primaryStage) throws Exception {


        setIcon(primaryStage);

        Image image_logo = new Image(__iconName);
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
        Class<DbApplication> helloApplicationClass = DbApplication.class;
        URL resource = helloApplicationClass.getResource("db-log-view.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        Parent loaded = fxmlLoader.load();
        DbLogController controller = fxmlLoader.getController();
        controller.init();
        Scene scene = new Scene(loaded, 1000, 600, false, SceneAntialiasing.BALANCED);
        primaryStage.setTitle(DbConfig.ins.serverName());
        primaryStage.setScene(scene);

        primaryStage.getIcons().add(image_logo);
        // primaryStage.initStyle(StageStyle.UNDECORATED);    // 可以隐藏任务栏上的图标
        primaryStage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            closeSelect(primaryStage);
        });
        /*todo 通过调用 http://localhost:19902/api/db/show */
        primaryStage.show();
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setAlwaysOnTop(false);
        RunAsync.async(() -> {
            try {
                /*必须让界面闪一下，不然程序不稳定，容易崩溃*/
                Thread.sleep(1000);

                ApplicationMain.guiCountDownLatch.countDown();
                if (StringUtils.isBlank(GraalvmUtil.javaClassPath())) {
                    closeSelect(primaryStage);
                }
            } catch (InterruptedException ignore) {}
        });
    }

    /** 开启系统托盘图标 */
    public void setIcon(Stage primaryStage) {
        try {
            if (SystemTray.isSupported()) {
                /*TODO 系统托盘图标*/
                SystemTray tray = SystemTray.getSystemTray();
                BufferedImage bufferedImage = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(__iconName));
                TrayIcon trayIcon = new TrayIcon(bufferedImage, DbConfig.ins.serverName());
                trayIcon.setImageAutoSize(true);

                /*TODO 图标双击事件 */
                trayIcon.addActionListener(event -> {
                    WebService.getIns().getShowWindow().run();
                });
                tray.add(trayIcon);
                icon_checked.set(true);
                log.warn("创建托盘图标");
            } else {
                log.warn("当前系统不允许创建托盘图标");
            }
        } catch (Throwable e) {
            log.error("setIcon failed ", e);
        }
    }


    public void closeSelect(Stage primaryStage) {
        if (icon_checked.get()) {
            PlatformImpl.runLater(() -> primaryStage.hide());
        } else {
            PlatformImpl.runLater(() -> primaryStage.setIconified(true));
        }

        // Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        // alert.setTitle("提示");
        // alert.setHeaderText("点击按钮以确认你的选择!");
        // // alert.setContentText("点击按钮以确认你的选择。");
        //
        // ButtonType buttonTypeOk = new ButtonType("关闭进程", ButtonBar.ButtonData.YES);
        // ButtonType buttonTypeCancel = new ButtonType("后台运行", ButtonBar.ButtonData.OK_DONE);
        //
        // alert.getButtonTypes().setAll(buttonTypeCancel, buttonTypeOk);
        //
        // ButtonType result = alert.showAndWait().orElse(buttonTypeCancel);
        //
        // if (result == buttonTypeOk) {
        // Runtime.getRuntime().halt(0);
        // } else {
        //     primaryStage.hide();
        // }
    }

    public static void alert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(content);
        alert.showAndWait();
    }
}