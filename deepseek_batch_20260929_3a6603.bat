@echo off
chcp 65001 >nul
cd /d D:\Projects\SConnect

set SAMSUNG=adb-R58R33JHTWB-RZkS2K._adb-tls-connect._tcp
set REALME=adb-EE440L027042-jkibRP._adb-tls-connect._tcp
set APK=app\build\outputs\apk\debug\app-debug.apk
set PACKAGE=com.adel.s_connect/.MainActivity

echo ========================================
echo    S-Connect: установка на оба
echo ========================================
echo.

echo [1/2] Установка на Samsung...
adb -s %SAMSUNG% install -r %APK%
if errorlevel 1 (
    echo ❌ Samsung: ошибка
) else (
    echo ✅ Samsung: установлено
    adb -s %SAMSUNG% shell am start -n %PACKAGE%
)
echo.

echo [2/2] Установка на Realme...
adb -s %REALME% install -r %APK%
if errorlevel 1 (
    echo ❌ Realme: ошибка
) else (
    echo ✅ Realme: установлено
    adb -s %REALME% shell am start -n %PACKAGE%
)
echo.

echo ========================================
echo    Готово!
echo ========================================
pause