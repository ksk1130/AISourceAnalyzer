@echo off
set JAVA_HOME=C:\bin\jdk-21

%JAVA_HOME%\bin\java -cp "app\build\classes\java\main;libs\*" -Dfile.encoding=UTF-8 -Xmx2048m -Xms2048m -XX:+UseG1GC org.example.App %*
