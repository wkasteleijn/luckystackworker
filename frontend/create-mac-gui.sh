if [ "$#" -ne 1 ]; then
  echo "Usage: $0 architecture (x64 or arm64)"
  exit 1
fi

rm -rf lsw-gui-darwin-$1
npx electron-packager . lsw-gui --platform darwin --arch $1 --icon ../luckystackworker_icon.icns --overwrite
cd ./lsw-gui-darwin-$1/lsw-gui.app/Contents
rm -rf Versions
cd ./Resources/app
rm -rf node_modules
rm -rf src
rm -rf .angular
rm -rf createExe*.bat
rm -rf launch4j_lsw.xml
rm -rf linux
rm -f LuckyStackWorker.exe

