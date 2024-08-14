package db712.server;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MyDB extends DB {

    private String data_base;
    private String user;
    private String pwd;
    int bak_Time = 0;
    Timer timer = new Timer();

    public MyDB(DBConfiguration config, String data_base, String user, String pwd, int bak_Time) {
        super(config);
        this.data_base = data_base;
        this.user = user;
        this.pwd = pwd;
        try {
            this.prepareDirectories();
        } catch (Exception e) {
            if (!check(e))
                log.error("prepareDirectories failed " + e);
        }
        try {
            this.unpackEmbeddedDb();
        } catch (Exception ignore) {}
        try {
            this.install();
        } catch (Exception e) {
            if (!check(e))
                log.error("prepareDirectories failed " + e);
        }
    }

    private boolean check(Throwable e) {
        if (e != null) {
            if (e.getMessage().contains("is not empty. Only new or empty existing directories are accepted for --datadir"))
                return true;
            if (e.getCause() != null && check(e.getCause())) {
                return true;
            }
        }
        return false;
    }

    public void initDb() {
        try {
            String[] split = data_base.split(",|，");
            for (String dbName : split) {
                createDB(dbName, user, pwd);
            }

        } catch (Exception e) {
            log.error("prepareDirectories failed " + e);
        }
        initBakTimer();
    }

    public void initBakTimer() {
        if (bak_Time > 0) {

            timer.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override public void run() {
                            bakSql();
                        }
                    },
                    TimeUnit.MINUTES.toMillis(bak_Time),
                    TimeUnit.MINUTES.toMillis(bak_Time)
            );

        }
    }

    public void bakSql() {
        try {
            String format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS").format(new Date());

            String[] split = data_base.split(",|，");
            for (String dbName : split) {
                File outputFile = new File("data-base/bak/" + dbName + "/" + format + ".sql");
                outputFile.getParentFile().mkdirs();
                ManagedProcess managedProcess = dumpSQL(
                        outputFile,
                        dbName, user, pwd
                );
                try {
                    managedProcess.start();
                    managedProcess.waitForExit();
                } catch (Exception ignore) {
                }
                try {
                    managedProcess.destroy();
                } catch (Exception ignore) {}
                log.info("备份：" + outputFile);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override public synchronized void start() throws ManagedProcessException {
        super.start();
        initDb();
    }

    @Override public synchronized void stop() throws ManagedProcessException {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        /*这里会卡*/
        // bakSql();
        super.stop();
    }

}
