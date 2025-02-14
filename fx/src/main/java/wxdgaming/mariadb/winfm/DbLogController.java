package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import wxdgaming.mariadb.server.DBFactory;
import wxdgaming.mariadb.server.RunAsync;
import wxdgaming.mariadb.server.WebService;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


@Slf4j
public class DbLogController {

    public MenuItem mi_start;
    public MenuItem mi_stop;
    public TextArea text_area;

    AtomicBoolean scrollLocked = new AtomicBoolean(false);
    AtomicBoolean openOutput = new AtomicBoolean(true);
    AtomicReference<Supplier<String>> openFilter = new AtomicReference<>();

    Thread hook;

    AtomicReference<String> oldFind = new AtomicReference<>("");
    AtomicInteger findIndex = new AtomicInteger(0);

    ReentrantLock lock = new ReentrantLock();
    StringBuilder stringBuilder = new StringBuilder();

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

                @Override public void print(Object obj) {
                    print(String.valueOf(obj));
                }

                @Override public void print(String x) {
                    if (!openOutput.get()) return;
                    lock.lock();
                    try {
                        String[] greps = null;
                        if (openFilter.get() != null) {
                            String string = openFilter.get().get();
                            if (string != null && !string.trim().isEmpty()) {
                                greps = string.split(" ");
                            }
                        }
                        try (StringReader strReader = new StringReader(x);
                             BufferedReader bufferedReader = new BufferedReader(strReader);) {
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                if (StringUtils.isNotBlank(line)) {
                                    if (greps != null) {
                                        if (!Arrays.stream(greps).allMatch(line::contains)) {
                                            continue;
                                        }
                                    }
                                }
                                if (!stringBuilder.isEmpty()) stringBuilder.append("\n");
                                stringBuilder.append(line);
                            }
                        } catch (Throwable ignore) {}
                    } finally {
                        lock.unlock();
                    }
                }
            };
            System.setOut(printStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        RunAsync.async(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(10);
                    final String x;
                    lock.lock();
                    try {
                        if (!stringBuilder.isEmpty()) {
                            x = stringBuilder.toString();
                            stringBuilder = new StringBuilder();
                        } else {
                            continue;
                        }
                    } finally {
                        lock.unlock();
                    }
                    PlatformImpl.runAndWait(() -> {
                        /*一次性追加文本，这样可以减少卡顿*/
                        if (text_area.getLength() > 0)
                            text_area.appendText("\n");
                        text_area.appendText(x);
                        removeTopLine();
                    });
                } catch (Exception ignored) {}
            }
        });

        // 创建自定义的上下文菜单
        ContextMenu contextMenu = new ContextMenu();
        // 创建菜单项
        MenuItem copyMenuItem = new MenuItem("复制");
        {
            copyMenuItem.setDisable(true);
            copyMenuItem.setOnAction(event -> text_area.copy());
            contextMenu.getItems().add(copyMenuItem);
            MenuItem selectAllMenuItem = new MenuItem("全选");
            selectAllMenuItem.setOnAction(event -> text_area.selectAll());
            contextMenu.getItems().add(selectAllMenuItem);
        }
        contextMenu.getItems().add(new SeparatorMenuItem());
        {
            MenuItem openFilterMenuItem = new MenuItem("关键词过滤");
            openFilterMenuItem.setOnAction(event -> {
                openFilterWindow();
            });
            contextMenu.getItems().add(openFilterMenuItem);
            MenuItem findMenuItem = new MenuItem("搜索");
            findMenuItem.setOnAction(event -> {
                scrollLocked.set(true);
                openFindWindow();
            });
            contextMenu.getItems().add(findMenuItem);
        }
        contextMenu.getItems().add(new SeparatorMenuItem());
        {
            {
                MenuItem clearMenuItem = new MenuItem("锁屏");
                clearMenuItem.setOnAction(event -> scrollLocked.set(true));
                contextMenu.getItems().add(clearMenuItem);
            }
            {
                MenuItem clearMenuItem = new MenuItem("滚屏");
                clearMenuItem.setOnAction(event -> scrollLocked.set(false));
                contextMenu.getItems().add(clearMenuItem);
            }
            {
                MenuItem clearMenuItem = new MenuItem("清屏");
                clearMenuItem.setOnAction(event -> {
                    scrollLocked.set(false);
                    clearOut();
                });
                contextMenu.getItems().add(clearMenuItem);
            }
        }
        contextMenu.getItems().add(new SeparatorMenuItem());
        {
            MenuItem pauseMenuItem = new MenuItem("暂停输出");
            MenuItem recoverMenuItem = new MenuItem("恢复输出");
            recoverMenuItem.setDisable(true);
            contextMenu.getItems().add(pauseMenuItem);
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

        // RunAsync.async(() -> {
        //     for (; ; ) {
        //         try {
        //             Thread.sleep(300);
        //         } catch (InterruptedException e) {
        //             e.printStackTrace();
        //         }
        //         System.out.println(randomString());
        //     }
        // });

    }

    public void openFilterWindow() {
        PlatformImpl.runLater(() -> {
            try {
                // 加载新的FXML文件
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/wxdgaming/mariadb/winfm/filter.fxml"));
                Parent root = loader.load();
                FilterController controller = loader.getController();
                // 创建新的Stage
                Stage newStage = new Stage();
                newStage.setTitle("过滤");
                // 设置舞台样式为无装饰，去掉系统默认的标题栏和按钮
                newStage.initStyle(StageStyle.UTILITY);
                newStage.setScene(new Scene(root));
                newStage.setResizable(false);
                // 显示新的Stage
                newStage.show();
                newStage.setAlwaysOnTop(true);

                newStage.setOnCloseRequest(windowEvent -> {
                    openFilter.set(null);
                });

                openFilter.set(() -> controller.txt_find.getText());

            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        });
    }

    public void openFindWindow() {
        PlatformImpl.runLater(() -> {
            try {
                // 加载新的FXML文件
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/wxdgaming/mariadb/winfm/find.fxml"));
                Parent root = loader.load();
                FindController controller = loader.getController();
                /*注册事件*/
                controller.init(this::find);
                // 创建新的Stage
                Stage newStage = new Stage();
                // 设置舞台样式为无装饰，去掉系统默认的标题栏和按钮
                newStage.initStyle(StageStyle.UTILITY);
                newStage.setTitle("查找");
                newStage.setScene(new Scene(root));
                newStage.setResizable(false);
                // 显示新的Stage
                newStage.show();
                newStage.setAlwaysOnTop(true);

                newStage.setOnCloseRequest(windowEvent -> {
                    scrollLocked.set(false);
                });

            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        });
    }

    public String find(String findStr) {
        scrollLocked.set(true);
        String textAreaText = text_area.getText();
        if (Objects.equals(oldFind.get(), findStr)) {
            /*重新开始*/
            findIndex.set(0);
        }
        int indexOf = textAreaText.indexOf(findStr, findIndex.get());
        int allCounted = countCharacter(textAreaText, findStr, -1);
        if (indexOf > 0) {
            text_area.selectRange(indexOf, indexOf + findStr.length());
            findIndex.set(indexOf + findStr.length());
            int findCounted = countCharacter(textAreaText, findStr, indexOf + findStr.length());
            return "总共：" + allCounted + ", 第：" + findCounted + ", 位置：" + indexOf;
        } else {
            findIndex.set(0);
            return "总共：" + allCounted + ", 第：" + allCounted + " 结尾";
        }
    }

    public String randomString() {
        int len = (int) (Math.random() * 1000);

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

    /** 查找指定字符串数量 */
    public static int countCharacter(String str, String target, int endIndex) {
        int count = 0;
        int index = -1;
        for (; ; ) {
            if ((index = str.indexOf(target, index + 1)) >= 0 && (endIndex < 0 || index < endIndex)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private void removeTopLine() {
        /*一次性删除多余的字符，界面只更新一次，减少卡顿*/
        String text = text_area.getText();
        int delLine = countCharacter(text, "\n", -1) - 800;
        if (delLine < 1) {
            return;
        }
        int firstNewLineIndex = 0;
        for (int i = 0; i < delLine; i++) {
            firstNewLineIndex = text.indexOf('\n', firstNewLineIndex + 1);
        }
        if (firstNewLineIndex != -1) {
            text_area.deleteText(0, firstNewLineIndex + 1);
        }
        text_area.appendText(" ");
    }

    private void clearOut() {
        PlatformImpl.runAndWait(() -> {
            text_area.setText("");
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