if [ "$#" -ne 1 ]; then
  echo "Usage: $0 architecture (x64 or arm64)"
  exit 1
fi

if [ "$1" = "x64" ]; then
    echo "Building the LSW for $1 architecture"
    export JRE_HOME="/Users/wkasteleijn/Applications/jre21-zulu-x64/Java/zulu-21.jre/Contents/Home"
fi

if [ "$1" = "arm64" ]; then
    echo "Building the LSW for $1 architecture"
    export JRE_HOME="/Users/wkasteleijn/Applications/jre21-zulu-arm64/Java/zulu-21.jre/Contents/Home"
fi

rm -rf LuckyStackWorker-darwin-$1
npx electron-packager . LuckyStackWorker --platform darwin --arch $1 --icon luckystackworker_icon.icns --overwrite
mkdir ./LuckyStackWorker-darwin-$1/LuckyStackWorker.app/Contents/Java
cp ../backend/target/luckystackworker.jar ./LuckyStackWorker-darwin-$1/LuckyStackWorker.app/Contents/Java
cd ./LuckyStackWorker-darwin-$1/LuckyStackWorker.app/Contents
rm -rf Versions
mkdir ./Java/jre
cp -R $JRE_HOME/* ./Java/jre
cd ./Resources/app
chmod +x lsworker-mac.sh
rm -rf node_modules
rm -rf src
rm -rf .angular
rm -rf createExe*.bat
rm -rf launch4j_lsw.xml
rm -rf linux
rm -f LuckyStackWorker.exe
