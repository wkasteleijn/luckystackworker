rmdir /s /q LuckyStackWorker-win32-x64
npx electron-packager . LuckyStackWorker --platform=win32 --icon ./lsw_icon.ico --overwrite --no-asar --ignore="^/(src|node_modules|\.angular|LuckyStackWorker-linux-x64|linux|LuckyStackWorker-linux-x64\.zip|createExe.*\.bat|launch4j_lsw\.xml)"
