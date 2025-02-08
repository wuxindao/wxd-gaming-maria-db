package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
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
                        Platform.runLater(() -> {
                            String line = x;
                            /*委托给ui线程*/
                            try {
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
        clearListView();
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
        clearListView();
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

    private void clearListView() {
        PlatformImpl.runAndWait(() -> {
            text_area.setText("");
            textLineNumber.set(0);
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

}