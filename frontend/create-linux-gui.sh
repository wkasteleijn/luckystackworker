rm -rf LuckyStackWorker-linux-x64
npx electron-packager . LuckyStackWorker --platform linux --arch x64 --icon ../graphics/luckystackworker_icon.png --overwrite --no-asar --ignore="^/(jre|src|node_modules|\.angular|LuckyStackWorker-linux-x64|linux|LuckyStackWorker-linux-x64\.zip|createExe.*\.bat|launch4j_lsw\.xml)"
cp ../backend/build/libs/luckystackworker.jar ./LuckyStackWorker-linux-x64
