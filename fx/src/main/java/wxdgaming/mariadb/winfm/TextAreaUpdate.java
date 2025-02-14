package wxdgaming.mariadb.winfm;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.TextArea;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 更新内容
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-14 14:01
 **/
public class TextAreaUpdate extends Thread {

    private final TextArea textArea;
    /** textarea 文本框展示的最大行数 */
    private final int showMaxLine;
    /** 单次处理的行数 */
    private final int actionLine;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger totalLine = new AtomicInteger();
    private final LinkedBlockingQueue<String> strings = new LinkedBlockingQueue<>();
    private StringBuilder stringBuilder = new StringBuilder();

    /**
     * 构建 textarea 更新器
     *
     * @param textArea    文本框对象
     * @param showMaxLine 展示的最大行数
     * @param actionLine  单次处理的行数
     */
    public TextAreaUpdate(TextArea textArea, int showMaxLine, int actionLine) {
        this.textArea = textArea;
        this.showMaxLine = showMaxLine;
        this.actionLine = actionLine;
        this.setDaemon(true);
        this.start();
    }

    public void addText(String text) {
        lock.lock();
        try {
            if (stringBuilder.length() > 0) stringBuilder.append("\n");
            stringBuilder.append(text);
            totalLine.incrementAndGet();

            if (totalLine.get() > actionLine) {
                reset();
            }
        } finally {
            lock.unlock();
        }
    }

    private void reset() {
        strings.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        totalLine.set(0);
    }

    @Override public void run() {
        while (!this.isInterrupted()) {
            try {
                Thread.sleep(33);
                lock.lock();
                try {
                    if (strings.isEmpty() && stringBuilder.length() > 0) {
                        reset();
                    }
                } finally {
                    lock.unlock();
                }

                if (strings.isEmpty()) continue;

                String poll = strings.poll();

                PlatformImpl.runAndWait(() -> {
                    if (textArea.getLength() > 0)
                        textArea.appendText("\n");
                    textArea.appendText(poll);
                    removeTopLine();
                });
            } catch (Throwable ignored) {}
        }
    }

    private void removeTopLine() {
        String text = textArea.getText();
        int delLine = countCharacter(text, "\n", -1) - showMaxLine;
        if (delLine < 1) {
            return;
        }
        int firstNewLineIndex = 0;
        for (int i = 0; i < delLine; i++) {
            firstNewLineIndex = text.indexOf('\n', firstNewLineIndex + 1);
        }
        if (firstNewLineIndex != -1) {
            textArea.deleteText(0, firstNewLineIndex + 1);
        }
        textArea.appendText(" ");
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

}
