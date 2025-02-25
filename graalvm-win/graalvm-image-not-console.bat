@echo off

set option=-H:+UnlockExperimentalVMOptions
set option=%option% -H:+ReportExceptionStackTraces
set option=%option% -H:-ParseRuntimeOptions
set option=%option% -H:+AddAllCharsets
set option=%option% --enable-http
set option=%option% --enable-https
set option=%option% --no-fallback
set option=%option% --report-unsupported-elements-at-runtime
set option=%option% --allow-incomplete-classpath
@REM set option=%option% --link-at-build-time


set option=%option% --initialize-at-build-time=com.sh

:: build-time

set option=%option% --initialize-at-run-time=com.sh


::<!--新增-->

::<!--trace 表示编译时 进行 跟踪，有些情况下可能会报错，比如在这里设置了A类，但是A类没有MAIN方法 会导致报错-->

set option=%option% --trace-class-initialization=com.sun.beans.introspect.ClassInfo
set option=%option% --trace-class-initialization=com.sun.beans.introspect.MethodInfo
set option=%option% --trace-class-initialization=com.sun.beans.TypeResolver
set option=%option% --trace-class-initialization=java.beans.Introspector
set option=%option% --trace-class-initialization=java.beans.ThreadGroupContext
set option=%option% --trace-class-initialization=com.sh
set option=%option% --trace-object-instantiation=java.util.jar.JarFile


set option=%option% --add-exports=java.base/java.nio=ALL-UNNAMED
set option=%option% --add-opens java.base/java.nio=ALL-UNNAMED
set option=%option% -Dlogback.configurationFile=logback.xml
set option=%option% -Dfile.encoding=UTF-8
set option=%option% -Dio.netty.tryReflectionSetAccessible=true
:: 关闭控制台窗口
set option=%option% -H:NativeLinkerOption=/SUBSYSTEM:WINDOWS
:: 关闭控制台窗口
set option=%option% -H:NativeLinkerOption=/ENTRY:mainCRTStartup

echo %option%

C:\\java\\graalvm-jdk-21.0.6+8.1\\bin\\native-image.cmd %option% -H:ConfigurationFileDirectories=target/console/config -cp "target/console/mysql-server.jar;target/console/lib/*" -jar target/console/mysql-server.jar target/console/mysql-service