@echo off

:: 默认参数
set TYPE=normal
set PORT=8080

:: 参数个数
set "ARGS_LEN=0"
if not "%1" == "" set "ARGS_LEN=1"
if not "%2" == "" set "ARGS_LEN=2"

:: 如果为一个参数
if "%ARGS_LEN%" == "1" (
	echo %1|findstr "[0-9]*$" >nul
	echo %errorlevel%
	if %errorlevel% equ 0 (
		set "TYPE=%1"
	) else (
		set "PORT=%1"
	)
)
if "%ARGS_LEN%" == "2" (
	set "TYPE=%1"
	set "PORT=%2"
)

:: 启动
set "CURRENT_DIR=%cd%"
if not "%COREOS_HOME%" == "" goto filesql

set "COREOS_HOME=%CURRENT_DIR%"
if exist "%COREOS_HOME%\bin\start.bat" goto filesql

cd ..
set "COREOS_HOME=%cd%"
cd "%CURRENT_DIR%"


:filesql
del "%COREOS_HOME%\core\launcher.properties"
copy "%COREOS_HOME%\core\%TYPE%.properties" "%COREOS_HOME%\core\launcher.properties"

java -cp "%COREOS_HOME%/core/." -Dorg.osgi.service.http.port=%PORT% -Dos.home=%COREOS_HOME% aQute.launcher.pre.EmbeddedLauncher

