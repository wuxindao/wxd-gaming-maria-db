package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.web.WebView;
import org.apache.commons.io.IOUtils;
import wxdgaming.mariadb.server.RunAsync;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Htmlview
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-25 09:09
 **/
public class Htmlview {

    public WebView webview;

    public Htmlview() {
    }

    public String readHtml() {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream("/consolebox.html")) {
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("加载文件失败");
        }
    }

    public void init() {
        webview.getEngine().loadContent(readHtml());
        webview.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                RunAsync.async(() -> {
                    for (int i = 0; i < 10000; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {}
                        String s = DbLogController.randomString();
                        String script = "append('" + i + " " + s + "');";
                        PlatformImpl.runAndWait(() -> {
                            webview.getEngine().executeScript(script);
                        });
                    }
                });
            }
        });

    }

}
