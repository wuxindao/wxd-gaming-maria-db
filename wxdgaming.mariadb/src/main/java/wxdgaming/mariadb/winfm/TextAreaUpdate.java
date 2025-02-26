package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 更新内容
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-14 14:01
 **/
public class TextAreaUpdate extends Thread {

    private final WebView webView;
    private final long duration;
    /** 单次处理的行数 */
    private final int actionLine;

    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedBlockingQueue<List<String>> strings = new LinkedBlockingQueue<>();
    private List<String> stringBuilder;

    /**
     * 构建 textarea 更新器
     *
     * @param webView    文本框对象
     * @param actionLine 单次处理的行数
     */
    public TextAreaUpdate(WebView webView, long duration, int actionLine) {
        this.webView = webView;
        this.duration = duration;
        this.actionLine = actionLine;
        this.setPriority(Thread.MAX_PRIORITY);
        this.setDaemon(true);
        this.start();
        stringBuilder = new ArrayList<>(actionLine);
    }

    public void addText(String text) {
        lock.lock();
        try {
            stringBuilder.add(text);

            if (stringBuilder.size() >= actionLine) {
                reset();
            }
        } finally {
            lock.unlock();
        }
    }

    private void reset() {
        strings.add(stringBuilder);
        stringBuilder = new ArrayList<>(actionLine);
    }

    @Override public void run() {
        while (!this.isInterrupted()) {
            try {
                Thread.sleep(duration);
                lock.lock();
                try {
                    if (strings.isEmpty() && !stringBuilder.isEmpty()) {
                        reset();
                    }
                } finally {
                    lock.unlock();
                }

                if (strings.isEmpty()) continue;

                List<String> poll = strings.poll();
                for (String line : poll) {
                    PlatformImpl.runAndWait(() -> {
                        try {
                            String escapedLine = StringEscapeUtils.escapeEcmaScript(line);
                            webView.getEngine().executeScript("append(\"" + escapedLine + "\");");
                        } catch (Exception e) {
                            System.err.println(line);
                            e.printStackTrace(System.err);
                        }
                    });
                }
            } catch (Throwable ignored) {}
        }
    }

}
