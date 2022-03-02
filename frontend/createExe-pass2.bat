COPY ..\backend\target\luckystackworker-1.3.0.jar .\LuckyStackWorker-win32-x64\luckystackworker.jar
COPY ..\backend\lsw_db.mv.db .\LuckyStackWorker-win32-x64\
COPY .\LuckyStackWorker-win32-x64\\resources\app\LICENSE_LSW.txt .\LuckyStackWorker-win32-x64
COPY .\LuckyStackWorker-win32-x64\\resources\app\release_notes.txt .\LuckyStackWorker-win32-x64
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\src
RMDIR /Q /S .\LuckyStackWorker-win32-x64\resources\app\node_modules
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\createExe*.bat
DEL /Q .\LuckyStackWorker-win32-x64\resources\app\reset_db.sql
