package wxdgaming.mariadb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GraalvmUtil {

    public static final File ok = new File("db-ok.txt");

    public static void write(int state, String msg) {
        String json = """
                {"PID": %s, "states": %s, "web-port": %s, "msg": "%s"}
                """.formatted(fetchProcessId(), state, DbConfig.ins.getWebPort(), msg);
        writeFile(ok, json);
    }

    public static void writeFile(File file, String content) {
        try {
            Files.writeString(
                    file.toPath(),
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
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

    public static void initGui() {
        try {
            String cmdFilePath = "show-log.cmd";
            File file = new File(cmdFilePath);
            if (!file.exists()) {
                return;
            }
            // 构建包含 start 命令的命令数组
            String[] command = {"cmd.exe", "/c", "start", "cmd.exe", "/c", cmdFilePath};
            ProcessBuilder sh = new ProcessBuilder(command);
            asyncExeLocalCommand(null, sh);
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static void asyncExeLocalCommand(File file, ProcessBuilder pb) throws IOException {
        // 不使用Runtime.getRuntime().exec(command)的方式,因为无法设置以下特性
        // Java执行本地命令是启用一个子进程处理,默认情况下子进程与父进程I/O通过管道相连(默认ProcessBuilder.Redirect.PIPE)
        // 当服务执行自身重启的命令时,父进程关闭导致管道连接中断,将导致子进程也崩溃,从而无法完成后续的启动
        // 解决方式,(1)设置子进程IO输出重定向到指定文件;(2)设置属性子进程的I/O源或目标将与当前进程的相同,两者相互独立
        if (file == null || !file.exists()) {
            // 设置属性子进程的I/O源或目标将与当前进程的相同,两者相互独立
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        } else {
            // 设置子进程IO输出重定向到指定文件
            // 错误输出与标准输出,输出到一块
            pb.redirectErrorStream(true);
            // 设置输出日志
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
        }
        // 执行命令进程
        Process start = pb.start();
        try {
            start.waitFor();
            start.destroy();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String javaClassPath() {
        return System.getProperty("java.class.path");
    }

    public static List<String> jarResources() throws Exception {
        List<String> resourcesPath = new ArrayList<>();
        String x = javaClassPath();
        String[] split = x.split(File.pathSeparator);
        List<String> collect = Arrays.stream(split).sorted().collect(Collectors.toList());
        for (String string : collect) {

            Path start = Paths.get(string);
            if (!string.endsWith(".jar") && !string.endsWith(".war") && !string.endsWith(".zip")) {
                if (string.endsWith("classes")) {
                    try (Stream<Path> stream = Files.walk(start)) {
                        String target = start.toString();
                        stream
                                .map(Path::toString)
                                .filter(s -> s.startsWith(target) && s.length() > target.length())
                                .map(s -> {
                                    String replace = s.replace(target + File.separator, "");
                                    if (replace.endsWith(".class")) {
                                        replace = replace.replace(".class", "").replace(File.separator, ".");
                                    }
                                    return replace;
                                })
                                .forEach(resourcesPath::add);
                    }
                    continue;
                }
                System.out.println(string);
                continue;
            }

            try (InputStream inputStream = Files.newInputStream(start);
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry nextEntry = null;
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    /* todo 读取的资源字节可以做解密操作 */
                    resourcesPath.add(nextEntry.getName());
                }
            }
        }
        Collections.sort(resourcesPath);
        return resourcesPath;
    }

    public static class Tuple<F, S> {

        public final F f;
        public final S s;

        public Tuple(F f, S s) {
            this.f = f;
            this.s = s;
        }

    }

}
