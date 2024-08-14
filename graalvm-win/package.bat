@echo off
call graalvm-win\graalvm-package.bat
@REM call graalvm-win\graalvm-package-console.bat
@REM call graalvm-win\graalvm-image-console.bat
@REM call graalvm-win\graalvm-image-not-console.bat
call graalvm-win\graalvm-package-win.bat
call graalvm-win\graalvm-image-win.bat