package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.WebService;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class DbLogController {

    public MenuItem mi_start;
    public MenuItem mi_stop;
    public TextArea text_area;
    public TextField txt_grep;

    AtomicBoolean openOutput = new AtomicBoolean(true);
    AtomicBoolean openFilter = new AtomicBoolean(true);

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
                        if (!openOutput.get()) return;
                        /*委托给ui线程*/
                        Platform.runLater(() -> {
                            String line = x;
                            try {
                                if (openFilter.get()) {
                                    if (txt_grep.getText() != null && !txt_grep.getText().trim().isEmpty()) {
                                        String[] split = txt_grep.getText().split(" ");
                                        for (String grep : split) {
                                            if (!line.contains(grep)) {
                                                return;
                                            }
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

        // 创建自定义的上下文菜单
        ContextMenu contextMenu = new ContextMenu();
        // 创建菜单项
        MenuItem copyMenuItem = new MenuItem("复制");
        {
            copyMenuItem.setDisable(true);
            copyMenuItem.setOnAction(event -> text_area.copy());

            contextMenu.getItems().add(copyMenuItem);
        }
        {
            // MenuItem pasteMenuItem = new MenuItem("粘贴");
            // pasteMenuItem.setOnAction(event -> text_area.paste());
            // contextMenu.getItems().add(new SeparatorMenuItem());
            // contextMenu.getItems().add(pasteMenuItem);

        }
        {
            MenuItem selectAllMenuItem = new MenuItem("全选");
            selectAllMenuItem.setOnAction(event -> text_area.selectAll());
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(selectAllMenuItem);
        }
        {
            MenuItem clearMenuItem = new MenuItem("清屏");
            clearMenuItem.setOnAction(event -> clearOut());
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(clearMenuItem);
        }

        {
            MenuItem openFilterMenuItem = new MenuItem("启用关键词过滤");
            MenuItem closeFilterMenuItem = new MenuItem("关闭关键词过滤");
            openFilterMenuItem.setDisable(true);
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(openFilterMenuItem);
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(closeFilterMenuItem);

            openFilterMenuItem.setOnAction(event -> {
                openFilter.set(true);
                openFilterMenuItem.setDisable(true);
                closeFilterMenuItem.setDisable(false);
            });
            closeFilterMenuItem.setOnAction(event -> {
                openFilter.set(false);
                closeFilterMenuItem.setDisable(true);
                openFilterMenuItem.setDisable(false);
            });

        }

        {
            MenuItem pauseMenuItem = new MenuItem("暂停输出");
            MenuItem recoverMenuItem = new MenuItem("恢复输出");
            recoverMenuItem.setDisable(true);
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(pauseMenuItem);
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(recoverMenuItem);

            pauseMenuItem.setOnAction(event -> {
                openOutput.set(false);
                pauseMenuItem.setDisable(true);
                recoverMenuItem.setDisable(false);
            });
            recoverMenuItem.setOnAction(event -> {
                openOutput.set(true);
                recoverMenuItem.setDisable(true);
                pauseMenuItem.setDisable(false);
            });

        }

        // 禁用默认的上下文菜单
        text_area.setContextMenu(contextMenu);

        {
            // 为 selectedTextProperty 添加监听器
            text_area.selectedTextProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isBlank()) {
                    copyMenuItem.setDisable(true);
                } else {
                    copyMenuItem.setDisable(false);
                }
            });
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
    private void bakAction(ActionEvent event) {
        DBFactory.getIns().getMyDB().bakSql();
    }

    @FXML
    private void sourceAction(ActionEvent event) {
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
        File selectedFile = fileChooser.showOpenDialog(text_area.getScene().getWindow());
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
        clearOut();
        PlatformImpl.runAndWait(() -> {
            mi_start.setDisable(false);
            mi_stop.setDisable(true);
        });
        clearFile("data-base/data");
    }

    public void clearFile(String path) {
        CompletableFuture.runAsync(() -> {
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