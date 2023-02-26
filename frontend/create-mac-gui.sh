rm -rf sw-gui-darwin-x64
npx electron-packager . lsw-gui --icon ../luckystackworker_icon.icns --overwrite
cd ./lsw-gui-darwin-x64/lsw-gui.app/Contents/Resources/app
rm -rf node_modules
rm -rf src
rm -rf .angular
rm -rf createExe*.bat
rm -rf reset_db.sql
rm -rf launch4j_lsw.xml


