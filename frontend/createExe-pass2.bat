COPY ..\backend\target\luckystackworker.jar .\LuckyStackWorker-win32-x64\luckystackworker.jar
COPY .\LuckyStackWorker-win32-x64\\resources\app\LICENSE_LSW.txt .\LuckyStackWorker-win32-x64
COPY .\LuckyStackWorker-win32-x64\\resources\app\release_notes.txt .\LuckyStackWorker-win32-x64
COPY .\LuckyStackWorker-win32-x64\\resources\app\lsworker.bat .\LuckyStackWorker-win32-x64
MKDIR .\LuckyStackWorker-win32-x64\gmic
XCOPY ..\backend\gmic .\LuckyStackWorker-win32-x64\gmic /E /H /C /I
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\src
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\node_modules
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\.angular
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\LuckyStackWorker-linux-x64
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\linux
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\LuckyStackWorker-linux-x64.zip
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\createExe*.bat
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\launch4j_lsw.xml
