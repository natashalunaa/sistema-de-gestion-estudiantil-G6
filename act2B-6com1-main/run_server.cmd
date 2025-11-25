@echo off
setlocal

:: Cambia al directorio del proyecto si es necesario
:: cd /d "C:\ruta\a\tu\proyecto"

echo === Ejecutando mvn clean ===
call mvn clean
if errorlevel 1 (
    echo [ERROR] mvn clean falló.
    goto end
)

echo === Ejecutando mvn test ===
call mvn test
if errorlevel 1 (
    echo [ERROR] mvn test falló.
    goto end
)

echo === Ejecutando mvn exec:java ===
call mvn exec:java
if errorlevel 1 (
    echo [ERROR] mvn exec:java falló.
    goto end
)

:end
echo.
echo ================================
echo Proceso finalizado. La consola permanece abierta.
echo Presiona Ctrl+C para salir manualmente.
echo ================================
pause >nul