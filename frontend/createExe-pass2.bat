MOVE lsw_gui-win32-x64 LuckyStackWorker-win32-x64
COPY ..\backend\target\luckystackworker-4.3.0-beta.jar .\LuckyStackWorker-win32-x64\luckystackworker.jar
COPY .\LuckyStackWorker.exe .\LuckyStackWorker-win32-x64
COPY ..\backend\lsw_db.mv.db .\LuckyStackWorker-win32-x64\
COPY .\LuckyStackWorker-win32-x64\\resources\app\LICENSE_LSW.txt .\LuckyStackWorker-win32-x64
COPY .\LuckyStackWorker-win32-x64\\resources\app\release_notes.txt .\LuckyStackWorker-win32-x64
COPY C:\Users\wkast\AppData\Local\LuckyStackWorker\lsw_db.mv.db ..\backend
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\src
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\node_modules
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\.angular
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\createExe*.bat
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\reset_db.sql
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\launch4j_lsw.xml
