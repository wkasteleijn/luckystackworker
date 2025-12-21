set JAVA_HOME=resources\app\jre
set PATH=%JAVA_HOME%\bin;%PATH%
%JAVA_HOME%\bin\java.exe -Dspring.profiles.active=win -jar luckystackworker.jar > NUL 2>&1
