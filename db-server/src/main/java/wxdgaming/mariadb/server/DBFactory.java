package wxdgaming.mariadb.server;

import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * 数据库工厂
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-07-23 16:45
 **/
@Slf4j
@Getter
public class DBFactory {

    @Getter private static final DBFactory ins = new DBFactory();
    public static final File ok = new File("db-ok.txt");

    DBFactory() {}

    private DBConfigurationBuilder configBuilder;
    private MyDB myDB;

    public void init(String dataBase, int port, String user, String pwd) throws Exception {
        configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(port); // OR, default: setPort(0); => autom. detect free port
        configBuilder.setBaseDir("data-base/"); // just an example
        configBuilder.setDataDir("data-base/data");
        configBuilder.setDefaultCharacterSet("utf8mb4");

        myDB = new MyDB(configBuilder.build(), dataBase, user, pwd, 60);
        myDB.start();
        write(1);
    }

    public void sourceSql(String sqlFile) {
        File file = new File(sqlFile);
        String data_base = file.getParentFile().getName();
        try {
            myDB.source(new FileInputStream(sqlFile), myDB.getUser(), myDB.getPwd(), data_base);
            log.info("还原 {} {} 完成", data_base, sqlFile);
        } catch (Exception e) {
            log.error("还原文件：{} {}", data_base, sqlFile, e);
        }
    }

    public void write(int state) throws IOException {
        String json = "{\"PID\":" + fetchProcessId() + ", \"states\":" + state + ", \"web-port\":" + WebService.getIns().getPort() + "}";
        Files.write(
                ok.toPath(),
                json.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public void print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("====================================================").append("\n");
        stringBuilder.append("").append("\n");
        stringBuilder.append(" db service  - " + "127.0.0.1:" + myDB.getConfiguration().getPort()).append("\n");
        stringBuilder.append("web service  - " + "127.0.0.1:" + WebService.getIns().getPort()).append("\n");
        stringBuilder.append("").append("\n");
        stringBuilder.append("====================================================").append("\n");
        log.info(stringBuilder.toString());
    }


    public void stop() {
        try {
            write(0);
        } catch (Exception ignore) {}
        try {
            if (getMyDB() != null) {
                getMyDB().stop();
            }
        } catch (Throwable e) {
            log.info("db service close {}", e.toString());
        }
        log.info("db service close");
    }

    public static String fetchProcessId() {
        try {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            String name = runtime.getName();
            return name.substring(0, name.indexOf("@"));

        } catch (Exception ignore) {
            return "-1";
        }
    }
}
