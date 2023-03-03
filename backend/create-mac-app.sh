
# IMPORTANT!: UPDATE VERSION HERE FOR EVERY RELEASE!
export LSW_VERSION=3.1.0

cd ~/git/luckystackworker/backend/target
jar2app -r /Library/Java/JavaVirtualMachines/temurin-17.jre/Contents/Home -i ../../frontend/luckystackworker_icon.icns -n "LuckyStackWorker" \
    --jvm-options="-Dspring.profiles.active=mac -Dlsw.version=${LSW_VERSION}" luckystackworker-${LSW_VERSION}.jar
cd luckystackworker.app/Contents/MacOS
rm -rf JavaAppLauncher
cp ~/Applications/universalJavaApplicationStub-custom-src JavaAppLauncher
cp -r ~/git/luckystackworker/frontend/lsw-gui-darwin-x64/lsw-gui.app .
cp -f ~/.lsw/lsw_db.mv.db ../Resources
