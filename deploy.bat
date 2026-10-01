@echo off
cd /d D:\Projects\SConnect

echo === Сборка ===
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo Сборка упала. Логи смотри выше.
    exit /b 1
)

echo === Установка ===
adb -s adb-R58R33JHTWB-RZkS2K._adb-tls-connect._tcp install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo Установка упала.
    exit /b 1
)

echo === Запуск ===
adb -s adb-R58R33JHTWB-RZkS2K._adb-tls-connect._tcp shell am start -S -n com.adel.s_connect/.MainActivity

echo === Готово ===