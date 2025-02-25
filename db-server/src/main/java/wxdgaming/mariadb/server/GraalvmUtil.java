package wxdgaming.mariadb.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GraalvmUtil {

    static final Logger logger = LoggerFactory.getLogger(GraalvmUtil.class);

    /** ?????????? <br>?????????????????????У?<br>??????????config????<br>???????в???jar??????? TODO ???????????е??·????? */
    public static Stream<Tuple<String, InputStream>> resourceStreams(final String path) {
        return resourceStreams(Thread.currentThread().getContextClassLoader(), path);
    }

    /** ?????????? <br>?????????????????????У?<br>??????????config????<br>???????в???jar???????  TODO ???????????е??·????? */
    public static Stream<Tuple<String, InputStream>> resourceStreams(ClassLoader classLoader, final String path) {
        try {
            String findPath = path;
            boolean fileExists = new File(path).exists();
            if (!fileExists) {/*?????????????????????????*/
                URL resource = classLoader.getResource(path);
                if (resource != null) {
                    findPath = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8.toString());
                    if (findPath.startsWith("/")) {
                        findPath = findPath.substring(1);
                    }
                    if (findPath.contains(".zip!") || findPath.contains(".jar!")) {
                        findPath = findPath.substring(5, findPath.indexOf("!/"));
                        try (FileInputStream fileInputStream = new FileInputStream(findPath);
                             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);) {
                            List<Tuple<String, InputStream>> tmps = new ArrayList<>();
                            ZipEntry nextEntry = null;
                            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                                if (nextEntry.isDirectory()) continue;
                                String name = nextEntry.getName();
                                String replace = name.replace("\\", "/");
                                if (replace.startsWith(path)) {
                                    /* todo ??????????????????????? */
                                    byte[] extra = new byte[(int) nextEntry.getSize()];
                                    zipInputStream.read(extra, 0, (int) nextEntry.getSize());
                                    tmps.add(new Tuple<>(nextEntry.getName(), new ByteArrayInputStream(extra)));
                                }
                            }
                            return tmps.stream();
                        }
                    }
                }
            }

            return Files.walk(Paths.get(findPath), 999)
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .map(file -> {
                        try {
                            return new Tuple<>(file.getPath(), Files.newInputStream(file.toPath()));
                        } catch (Exception e) {
                            throw new RuntimeException("resources:" + file, e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("resources:" + path, e);
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
