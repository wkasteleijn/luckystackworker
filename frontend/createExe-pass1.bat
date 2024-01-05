rmdir /s /q LuckyStackWorker-win32-x64
npx electron-packager . lsw_gui --platform=win32 --icon ./lsw_icon.ico --overwrite
