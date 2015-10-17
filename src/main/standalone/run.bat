setlocal

@echo off

call settings.bat

@echo on

%JAVA_HOME%\bin\java -cp "resources;lib/*" com.netthreads.faceit.FaceFilter

endlocal