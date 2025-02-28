package wxdgaming.mariadb.server;


import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.mariadb.RunAsync;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-07-29 16:46
 **/
@Slf4j
@Getter
public class WebService {

    @Getter private static final WebService ins = new WebService();

    WebService() {}

    private HttpServer httpServer;

    HttpHandler httpHandler = exchange -> {
        byte[] bytes = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "x-requested-with,content-type");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().flush();
        exchange.close();
    };

    public void start(int port) throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);


        httpServer.createContext("/api/db/stop", exchange -> {
            httpHandler.handle(exchange);
            DBFactory.getIns().stop();
            Runtime.getRuntime().halt(0);
        });

        httpServer.createContext("/api/db/bak", exchange -> {
            httpHandler.handle(exchange);
            if (!DBFactory.getIns().isStarted()) {
                log.info("请先启动数据库服务");
                return;
            }
            DBFactory.getIns().getMyDB().bakSql();
        });

        httpServer.createContext("/api/db/check", exchange -> {
            httpHandler.handle(exchange);
        });

        httpServer.createContext("/api/db/clearance", exchange -> {
            httpHandler.handle(exchange);
            RunAsync.async(() -> {
                DBFactory.getIns().stop();
                clearFile("data-base/data");
                System.exit(1);
                Runtime.getRuntime().halt(0);
            });
        });
        httpServer.start();
        log.info("http://localhost:{}/api/db/stop", port);
        log.info("http://localhost:{}/api/db/check", port);
        log.info("http://localhost:{}/api/db/clearance", port);
    }

    public void clearFile(String path) {
        try {
            Path start = Paths.get(path);
            if (Files.exists(start)) {
                Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        log.info("清理文件：" + file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        log.info("清理文件：" + dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                log.info(path + " 文件夹不存在");
            }
        } catch (Exception e) {
            log.error("清档异常", e);
        }
        log.info("清档完成，需要手动启动");
    }

    public void stop() {
        httpServer.stop(0);
    }

}
