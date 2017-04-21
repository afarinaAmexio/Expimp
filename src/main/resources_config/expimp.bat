@echo off
 setLocal EnableDelayedExpansion
set JAVA_HOME=C:\PRODOM\JAVA\jdk1.7.0_71_x64
set ORIG=%cd%
set CLASSPATH=%~dp0config;%~dp0lib\*.jar;%~dp0ExpImp.jar
rem echo CLASSPATH : %CLASSPATH%
cd %~dp0
 for /R ./lib %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
 set CLASSPATH="!CLASSPATH!"
 rem echo !CLASSPATH!
java -cp "%CLASSPATH%" com.amexio.EIMain %1%
cd %ORIG%