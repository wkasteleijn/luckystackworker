if [ "$#" -ne 1 ]; then
  echo "Usage: $0 [architecture] (x64 or arm64)"
  exit 1
fi

if [ "$1" = "x64" ]; then
    echo "Building the LSW for $1 architecture"
    export JRE_HOME="/Users/wkasteleijn/Applications/jre21-zulu-x64/Java"
fi

if [ "$1" = "arm64" ]; then
    echo "Building the LSW for $1 architecture"
    export JRE_HOME="/Users/wkasteleijn/Applications/jre21-zulu-arm64/Java"
fi

cd ~/git/luckystackworker/backend/target
rm -rf LuckyStackWorker.app
jar2app -r $JRE_HOME -i ../../frontend/luckystackworker_icon.icns -n "LuckyStackWorker" \
    --jvm-options="-Dspring.profiles.active=mac" luckystackworker.jar
cd LuckyStackWorker.app/Contents/MacOS
rm -rf JavaAppLauncher
cp ~/Applications/universalJavaApplicationStub-custom-src JavaAppLauncher
cp -Ra ~/git/luckystackworker/frontend/lsw-gui-darwin-$1/lsw-gui.app .
