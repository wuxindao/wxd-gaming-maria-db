package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.RunAsync;
import wxdgaming.mariadb.server.WebService;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


@Slf4j
public class DbLogController {

    public MenuItem mi_start;
    public MenuItem mi_stop;
    public WebView webview;

    Thread hook;

    TextAreaUpdate textAreaUpdate;

    public DbLogController() throws Exception {

        hook = new Thread(() -> {
            DBFactory.getIns().stop();
            Runtime.getRuntime().halt(0);
        });

    }

    public void init() {
        try {
            webview.getEngine().loadContent(ApplicationMain.readHtml());
            textAreaUpdate = new TextAreaUpdate(webview, 1500, 50, 150);

            /*TODO 必须要等他初始化完成*/
            PrintStream printStream = new PrintStream(System.out) {

                @Override public void print(Object obj) {
                    print(String.valueOf(obj));
                }

                @Override public void print(String x) {
                    try (StringReader strReader = new StringReader(x);
                         BufferedReader bufferedReader = new BufferedReader(strReader);) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            textAreaUpdate.addText(line);
                        }
                    } catch (Throwable ignore) {}
                }
            };
            System.setOut(printStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // RunAsync.async(() -> {
        //     for (; ; ) {
        //         try {
        //             Thread.sleep(30);
        //         } catch (InterruptedException e) {
        //             e.printStackTrace();
        //         }
        //         System.out.println(randomString());
        //     }
        // });
    }


    public static String randomString() {
        int len = (int) (Math.random() * 200);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int randomIndex = (int) (Math.random() * 62);
            char randomChar = (char) (randomIndex < 10 ? '0' + randomIndex : (randomIndex < 36 ? 'a' + randomIndex - 10 : 'A' + randomIndex - 36));
            sb.append(randomChar);
        }
        return sb.toString();
    }

    @FXML
    private void startAction(ActionEvent event) {
        ApplicationMain.startDb(false);
        PlatformImpl.runAndWait(() -> {
            mi_start.setDisable(true);
            mi_stop.setDisable(false);
        });
    }

    @FXML
    private void stopAction(ActionEvent event) {
        PlatformImpl.runAndWait(() -> {
            DBFactory.getIns().stop();
            WebService.getIns().stop();
            System.out.println("停服完成");
            mi_start.setDisable(false);
            mi_stop.setDisable(true);
        });
    }

    @FXML
    private void bakAction(ActionEvent event) {
        if (!DBFactory.getIns().isStarted()) {
            DbApplication.alert("提示", "请先启动数据库服务");
            return;
        }
        DBFactory.getIns().getMyDB().bakSql();
    }

    @FXML
    private void sourceAction(ActionEvent event) {
        if (!DBFactory.getIns().isStarted()) {
            DbApplication.alert("提示", "请先启动数据库服务");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择 SQL 文件");
        File directory = new File("data-base/bak");
        if (!directory.exists()) {
            System.out.println("不存在备份");
            return;
        }
        fileChooser.setInitialDirectory(directory);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQL Files", "*.sql"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(webview.getScene().getWindow());
        if (selectedFile != null) {
            System.out.println("选择的文件路径: " + selectedFile.getAbsolutePath());
            // 你可以在这里添加代码来处理选择的文件
            DBFactory.getIns().sourceSql(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void clearAction(ActionEvent event) {
        DBFactory.getIns().stop();
        WebService.getIns().stop();
        webview.getEngine().executeScript("clearConsole();");
        PlatformImpl.runAndWait(() -> {
            mi_start.setDisable(false);
            mi_stop.setDisable(true);
        });
        clearFile("data-base/data");
    }

    public void clearFile(String path) {
        RunAsync.async(() -> {
            try {
                Path start = Paths.get(path);
                if (Files.exists(start)) {
                    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            System.out.println("清理文件：" + file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            System.out.println("清理文件：" + dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    System.out.println(path + " 文件夹不存在");
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
            System.out.println("清档完成，需要手动启动");
        });
    }


    @FXML
    private void closeAction(ActionEvent event) {
        DBFactory.getIns().stop();
        WebService.getIns().stop();
        Runtime.getRuntime().halt(0);
    }

    @FXML
    public void openLogFile(ActionEvent event) {
        String first = "target/logs/db.log";
        Path path = Paths.get(first).toAbsolutePath();

        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException ex) {
            log.error("文件：{}", path, ex);
        }
    }

    @FXML
    private void openConfig(ActionEvent event) {
        Path path = Paths.get("my.ini").toAbsolutePath();
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException ex) {
            log.error("文件：{}", path, ex);
        }
    }

    @FXML
    private void about(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        String content = """
                    数据库服务
                
                提供游戏数据库服务存储数据，
                
                在过滤框可以根据关键字过滤自己想要查看的内容，多个过滤词用空格隔开
                例如：
                输入字符串：我是测试字符串
                过滤词：我 字符
                """;
        alert.setHeaderText(content);
        alert.setContentText("版本：V1.0.10");
        alert.showAndWait();
    }

}