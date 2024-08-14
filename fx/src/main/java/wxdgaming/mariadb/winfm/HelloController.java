package wxdgaming.mariadb.winfm;

import wxdgaming.mariadb.server.DBFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class HelloController {

    @FXML private ListView<String> list_view;

    Thread hook;

    public HelloController() throws Exception {

        hook = new Thread(() -> {
            DBFactory.getIns().stop();
            Runtime.getRuntime().halt(0);
        });

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                {
                    /*TODO 必须要等他初始化完成*/
                    PrintStream printStream = new PrintStream(System.out) {
                        @Override public void print(String x) {
                            try {
                                Platform.runLater(() -> {
                                    String line = x;
                                    /*委托给ui线程*/
                                    try {
                                        while (list_view.getItems().size() > 300) {
                                            list_view.getItems().removeFirst();
                                        }
                                        if (line.length() > 300)
                                            line = line.substring(0, 300) + "......";
                                        list_view.getItems().add(line);
                                        list_view.scrollTo(list_view.getItems().size() + 999);
                                    } catch (Throwable ignore) {}
                                });
                            } catch (Throwable ignore) {}
                        }
                    };
                    System.setOut(printStream);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }



}