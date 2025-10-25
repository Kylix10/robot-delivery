@echo off
REM 切换到脚本所在目录
cd /d %~dp0
REM 后台启动 jar，不阻塞 bat
start "" java -jar target\Robot-delivery-0.0.1-SNAPSHOT.jar
