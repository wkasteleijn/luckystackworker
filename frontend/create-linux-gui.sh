if [ "$#" -ne 1 ]; then
  echo "Usage: $0 version"
  exit 1
fi

rm -rf LuckyStackWorker-linux-x64
npx electron-packager . lsw-gui --platform linux --arch x64 --icon ../luckystackworker_icon.png --overwrite
mv lsw-gui-linux-x64 LuckyStackWorker-linux-x64
cp ../backend/target/luckystackworker-$1.jar ./LuckyStackWorker-linux-x64
cp -R ./linux/* ./LuckyStackWorker-linux-x64
cd ./LuckyStackWorker-linux-x64/resources/app
rm -rf node_modules
rm -rf src
rm -rf .angular
rm -rf createExe*.bat
rm -rf reset_db.sql
rm -rf jre
rm -rf launch4j_lsw.xml
rm -rf LuckyStackWorker-win32-x64
rm -rf linux
rm -f LuckyStackWorker.exe
