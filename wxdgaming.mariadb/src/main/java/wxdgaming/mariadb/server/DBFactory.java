package wxdgaming.mariadb.server;

import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.mariadb.ApplicationMain;
import wxdgaming.mariadb.DbConfig;
import wxdgaming.mariadb.GraalvmUtil;

import java.io.File;
import java.io.FileInputStream;

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

    DBFactory() {}

    private DBConfigurationBuilder configBuilder;
    private MyDB myDB;
    private boolean started = false;

    public boolean init(String dataBase, int port, String user, String pwd) throws Exception {
        if (started) return false;
        configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(port); // OR, default: setPort(0); => autom. detect free port
        configBuilder.setBaseDir("data-base/"); // just an example
        configBuilder.setDataDir("data-base/data");
        configBuilder.setDefaultCharacterSet("utf8mb4");

        myDB = new MyDB(configBuilder.build(), dataBase, user, pwd, 60);
        myDB.start();
        GraalvmUtil.write(2, "成功");
        started = true;
        return true;
    }

    public void sourceSql(String sqlFile) {
        if (!started) {
            log.error("数据库未启动");
            return;
        }
        File file = new File(sqlFile);
        String data_base = file.getParentFile().getName();
        try {
            myDB.source(new FileInputStream(sqlFile), myDB.getUser(), myDB.getPwd(), data_base);
            log.info("还原 {} {} 完成", data_base, sqlFile);
        } catch (Exception e) {
            log.error("还原文件：{} {}", data_base, sqlFile, e);
        }
    }


    public void print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("====================================================").append("\n");
        stringBuilder.append("").append("\n");
        stringBuilder.append("               启动完成").append("\n");
        stringBuilder.append("").append("\n");
        stringBuilder.append(" db service  - " + "127.0.0.1:" + DbConfig.ins.getPort()).append("\n");
        stringBuilder.append("web service  - " + "127.0.0.1:" + DbConfig.ins.getWebPort()).append("\n");
        stringBuilder.append("").append("\n");
        stringBuilder.append("====================================================").append("\n");
        log.info(stringBuilder.toString());
    }


    public void stop() {
        try {
            GraalvmUtil.write(0, "停止");
        } catch (Exception ignore) {}
        try {
            if (getMyDB() != null) {
                getMyDB().stop();
            }
        } catch (Throwable e) {
            log.info("db service close {}", e.toString());
        }
        log.info("db service close");
        started = false;
    }


}
