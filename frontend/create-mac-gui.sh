if [ "$#" -ne 1 ]; then
  echo "Usage: $0 architecture (x64 or arm64)"
  exit 1
fi

if [ "$1" = "x64" ]; then
    echo "Building the LSW for $1 architecture"
    export JRE_HOME="/Users/wilcokasteleijn/Applications/zulu-25.jre.x64/Contents/Home"
fi

if [ "$1" = "arm64" ]; then
    echo "Building the LSW for $1 architecture"
    export JRE_HOME="/Users/wilcokasteleijn/Applications/zulu-25.jre.arm64/Contents/Home"
fi

rm -rf LuckyStackWorker-darwin-$1
npx electron-packager . LuckyStackWorker --platform darwin --arch $1 --icon ../graphics/luckystackworker_icon_mac --overwrite --no-asar --ignore="^/(src|node_modules|\.angular|LuckyStackWorker-linux-x64|linux|LuckyStackWorker-linux-x64\.zip|createExe.*\.bat|launch4j_lsw\.xml)"
mkdir ./LuckyStackWorker-darwin-$1/LuckyStackWorker.app/Contents/Java
cp ../backend/build/libs/luckystackworker.jar ./LuckyStackWorker-darwin-$1/LuckyStackWorker.app/Contents/Java
cd ./LuckyStackWorker-darwin-$1/LuckyStackWorker.app/Contents
rm -rf Versions
mkdir ./Java/jre
cp -R $JRE_HOME/* ./Java/jre
cd ./Resources/app
chmod +x lsworker-mac.sh
