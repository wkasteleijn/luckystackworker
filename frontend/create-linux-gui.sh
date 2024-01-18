rm -rf lsw-gui-linux-x64
npx electron-packager . lsw-gui --platform linux --arch x64 --icon ../luckystackworker_icon.png --overwrite
cd ./lsw-gui-linux-x64/resources/app
rm -rf node_modules
rm -rf src
rm -rf .angular
rm -rf createExe*.bat
rm -rf reset_db.sql
rm -rf jre
rm -rf launch4j_lsw.xml
