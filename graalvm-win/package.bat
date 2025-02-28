@echo off
call graalvm-win\graalvm-package.bat
REM 检查errorlevel的值
if %errorlevel% equ 0 (
    echo "graalvm-package.bat success。"
) else (
    echo "graalvm-package.bat error level: %errorlevel%"
    exit
)
call graalvm-win\graalvm-package-win.bat
REM 检查errorlevel的值
if %errorlevel% equ 0 (
    echo "graalvm-package-win.bat success。"
) else (
    echo "graalvm-package-win.bat error level: %errorlevel%"
    exit
)
call graalvm-win\graalvm-image-win.bat