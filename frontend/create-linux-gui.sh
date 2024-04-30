rm -rf LuckyStackWorker-linux-x64
npx electron-packager . lsw-gui --platform linux --arch x64 --icon ../luckystackworker_icon.png --overwrite
mv lsw-gui-linux-x64 LuckyStackWorker-linux-x64
cp ../backend/target/luckystackworker.jar ./LuckyStackWorker-linux-x64
cd ./LuckyStackWorker-linux-x64/resources/app
rm -rf node_modules
rm -rf src
rm -rf .angular
rm -rf createExe*.bat
rm -rf jre
rm -rf launch4j_lsw.xml
rm -rf LuckyStackWorker-win32-x64
rm -rf linux
rm -f LuckyStackWorker.exe
