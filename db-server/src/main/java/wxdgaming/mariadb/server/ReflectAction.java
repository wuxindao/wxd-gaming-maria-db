package wxdgaming.mariadb.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-07-15 17:51
 **/
@Slf4j
@Getter
public class ReflectAction {

    public static ReflectAction of() {
        return new ReflectAction();
    }

    private ReflectAction() {}

    private final Set<Class<?>> notConstructor0List = new HashSet<>();

    public void action(Class<?> cls, boolean checkPackage) {
        if (cls == Object.class) return;
        if (checkPackage && (!cls.getName().startsWith("db712")))
            return;
        if (cls.getAnnotation(GvmExclude.class) != null) return;
        if (cls.getSuperclass() != null) action(cls.getSuperclass(), checkPackage);

        actionField(cls);
        actionMethod(cls);
    }

    public void actionMethod(Class<?> cls) {
        {

            Constructor<?> defaultConstructor1 = null;
            try {
                defaultConstructor1 = cls.getDeclaredConstructor();
            } catch (NoSuchMethodException ignore) {}
            if (defaultConstructor1 == null) {
                notConstructor0List.add(cls);
            }
            Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
            for (Constructor<?> declaredConstructor : declaredConstructors) {
                if (Modifier.isStatic(declaredConstructor.getModifiers())) {
                    continue;
                }
                if (declaredConstructor.getAnnotation(GvmExclude.class) != null) continue;
                try {
                    Object findMethod = cls.getDeclaredConstructor(declaredConstructor.getParameterTypes());
                    log.info("reflectActionMethod: " + findMethod);
                } catch (Throwable ignore) {}
            }
        }
        {
            Method[] declaredMethods = cls.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getAnnotation(GvmExclude.class) != null) continue;
                try {
                    Method findMethod = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    log.info("reflectActionMethod: " + findMethod);
                } catch (Throwable ignore) {}
            }
        }
    }

    public void actionField(Class<?> cls) {
        Field[] declaredFields = cls.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getAnnotation(GvmExclude.class) != null) continue;
            try {
                log.info("reflectActionField: " + cls.getDeclaredField(field.getName()));
            } catch (Throwable ignore) {}
        }
    }

    /** 获取所有资源 <br>如果传入的目录本地文件夹没有，<br>会查找本地目录config目录，<br>如果还没有查找jar包内资源 TODO 开发者模式会有点点路径文件 */
    public static Stream<GraalvmUtil.Tuple<String, InputStream>> resourceStreams(final String path) {
        return resourceStreams(Thread.currentThread().getContextClassLoader(), path);
    }

    /** 获取所有资源 <br>如果传入的目录本地文件夹没有，<br>会查找本地目录config目录，<br>如果还没有查找jar包内资源  TODO 开发者模式会有点点路径文件 */
    public static Stream<GraalvmUtil.Tuple<String, InputStream>> resourceStreams(ClassLoader classLoader, final String path) {
        try {
            String findPath = path;
            boolean fileExists = true;
            if (!new File(path).exists()) {
                fileExists = false;
                if (!path.startsWith("/")) {
                    if (!path.startsWith("config/")) {
                        if (new File("config/" + path).exists()) {
                            findPath = "config/" + path;
                            fileExists = true;
                        }
                    }
                }
            }
            if (!fileExists) {/*当本地文件不存在才查找资源文件*/
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
                            List<GraalvmUtil.Tuple<String, InputStream>> tmps = new ArrayList<>();
                            ZipEntry nextEntry = null;
                            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                                if (nextEntry.isDirectory()) continue;
                                String name = nextEntry.getName();
                                String replace = name.replace("\\", "/");
                                if (replace.startsWith(path)) {
                                    /* todo 读取的资源字节可以做解密操作 */
                                    byte[] extra = new byte[(int) nextEntry.getSize()];
                                    zipInputStream.read(extra, 0, (int) nextEntry.getSize());
                                    tmps.add(new GraalvmUtil.Tuple<>(nextEntry.getName(), new ByteArrayInputStream(extra)));
                                }
                            }
                            return tmps.stream();
                        }
                    }
                }
            }

            return listFileStream(findPath, 999, null).map(file -> {
                try {
                    return new GraalvmUtil.Tuple<>(file.getPath(), Files.newInputStream(file.toPath()));
                } catch (Exception e) {
                    throw new RuntimeException("resources:" + file, e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("resources:" + path, e);
        }
    }

    public static Stream<File> listFileStream(String path, int maxDepth, Predicate<File> test) {
        return listFileStream(Paths.get(path), maxDepth, test);
    }

    public static Stream<File> listFileStream(Path path, int maxDepth, Predicate<File> test) {
        try {
            Stream<File> fileStream = Files.walk(path, maxDepth)
                    .map(Path::toFile).filter(File::isFile);
            if (test != null) {
                fileStream = fileStream.filter(test);
            }
            return fileStream;
        } catch (Exception e) {
            throw new RuntimeException(path.toString(), e);
        }
    }

}
