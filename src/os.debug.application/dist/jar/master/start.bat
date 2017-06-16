@echo off

:: 默认启动端口
set PORT=8080
:: 获取当前目录位置
set "COREOS_HOME=%cd%"

:: 如果脚本的第一个参数不为空,则重置PORT端口号
if not "%1" == "" set "PORT=%1"

:: 通过java -jar 启动程序,并指定占用的端口号和配置文件的位置
java -Dorg.osgi.service.http.port=%PORT% -Dos.home=%COREOS_HOME% -Dos.conf=%COREOS_HOME%\config.properties -jar master.jar