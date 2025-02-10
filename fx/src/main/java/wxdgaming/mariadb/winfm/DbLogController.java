package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.WebService;

import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class DbLogController {

    public javafx.scene.control.MenuItem mi_start;
    public MenuItem mi_stop;
    public TextArea text_area;
    public TextField txt_grep;

    AtomicInteger textLineNumber = new AtomicInteger(0);
    Thread hook;

    public DbLogController() throws Exception {

        hook = new Thread(() -> {
            DBFactory.getIns().stop();
            Runtime.getRuntime().halt(0);
        });

    }

    public void init() {
        try {
            /*TODO 必须要等他初始化完成*/
            PrintStream printStream = new PrintStream(System.out) {
                @Override public void print(String x) {
                    try {
                        /*委托给ui线程*/
                        Platform.runLater(() -> {
                            String line = x;
                            try {
                                if (txt_grep.getText() != null && !txt_grep.getText().trim().isEmpty()) {
                                    String[] split = txt_grep.getText().split(" ");
                                    for (String grep : split) {
                                        if (!line.contains(grep)) {
                                            return;
                                        }
                                    }
                                }
                                text_area.appendText(line);
                                text_area.appendText("\n");
                                textLineNumber.incrementAndGet();
                                if (textLineNumber.get() > 1500) {
                                    removeTopLine();
                                }
                            } catch (Throwable ignore) {}
                        });
                    } catch (Throwable ignore) {}
                }
            };
            System.setOut(printStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        clearOut();
        PlatformImpl.runAndWait(() -> {
            DBFactory.getIns().stop();
            WebService.getIns().stop();
            System.out.println("停服完成");
            mi_start.setDisable(false);
            mi_stop.setDisable(true);
        });
    }

    @FXML
    private void clearAction(ActionEvent event) {
        DBFactory.getIns().stop();
        WebService.getIns().stop();
        clearOut();
        PlatformImpl.runAndWait(() -> {
            mi_start.setDisable(false);
            mi_stop.setDisable(true);
        });
        CompletableFuture.runAsync(() -> {
            try {
                Files.walkFileTree(Paths.get("data-base"), new SimpleFileVisitor<Path>() {
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
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
            System.out.println("清档完成，需要手动启动");
        });
    }

    private void removeTopLine() {
        String text = text_area.getText();
        int firstNewLineIndex = text.indexOf('\n');
        if (firstNewLineIndex != -1) {
            String newText = text.substring(firstNewLineIndex + 1);
            text_area.setText(newText);
        } else {
            text_area.clear(); // 如果没有换行符，清空整个 TextArea
        }
        textLineNumber.decrementAndGet();
    }

    private void clearOut() {
        PlatformImpl.runAndWait(() -> {
            text_area.setText("");
            textLineNumber.set(0);
        });
    }

    @FXML
    private void clearOutAction(ActionEvent event) {
        clearOut();
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
        alert.setContentText("版本：V1.0.1");
        alert.showAndWait();
    }
}