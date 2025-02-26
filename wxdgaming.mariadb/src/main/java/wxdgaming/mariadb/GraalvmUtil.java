package wxdgaming.mariadb;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
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

    public static String readHtml() {
        try (InputStream resourceAsStream = ApplicationMain.class.getResourceAsStream("/consolebox.html")) {
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("加载文件失败");
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
