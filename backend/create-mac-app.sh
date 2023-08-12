if [ "$#" -ne 2 ]; then
  echo "Usage: $0 [lsw_version] [architecture] (x64 or arm64)"
  exit 1
fi

if [ "$2" = "x64" ]; then
    echo "Building the LSW for $2 architecture"
    export JRE_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jre/Contents/Home"
fi

if [ "$2" = "arm64" ]; then
    echo "Building the LSW for $2 architecture"
    export JRE_HOME="/Users/wkasteleijn/Applications/jre-arm64/Contents/Home"
fi

cd ~/git/luckystackworker/backend/target
rm -rf LuckyStackWorker.app
jar2app -r $JRE_HOME -i ../../frontend/luckystackworker_icon.icns -n "LuckyStackWorker" \
    --jvm-options="-Dspring.profiles.active=mac -Dlsw.version=$1" luckystackworker-$1.jar
cd LuckyStackWorker.app/Contents/MacOS
rm -rf JavaAppLauncher
cp ~/Applications/universalJavaApplicationStub-custom-src JavaAppLauncher
cp -Ra ~/git/luckystackworker/frontend/lsw-gui-darwin-$2/lsw-gui.app .
cp -f ~/.lsw/lsw_db.mv.db ../Resources
