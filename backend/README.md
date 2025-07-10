# LuckyStackWorker Backend

This readme serves as an instruction on how to get the project build and run locally. 

## Build

- Make sure you use the same version of the OpenJDK as this project. See the build.gradle file:
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of([CURRENT OPENJDK VERSION])
    }
}
```
- The build uses gradle so make sure to install it first. On MacOS:

```bash
brew install gradle
```

Next, since the app depends on a particular library version parallelcolt 0.11.4 that isn't available on any public repo
you need to download and compile the sources from github the first time. Download the source from:

[https://github.com/rwl/ParallelColt/releases/tag/parallelcolt-0.11.4](https://github.com/rwl/ParallelColt/releases/tag/parallelcolt-0.11.4)

- In case you don't have it, make sure to install Open JDK version 8, which is needed to compile this old version of the library. For example you can download it from
Azul zulu: [https://www.azul.com/downloads](https://www.azul.com/downloads). Make sure to download the zip and only use it temporarily, as LuckyStackWorker usually depends on the latest LTS of OpenJDK.
- Unzip the asset, compile the project, create copy the jar to a permanent installation folder: 
```bash
cd [PARALLELCOLT UNZIPPED FOLDER]
export JAVA_HOME=[ZULU OPENJDK INSTALLATION FOLDER]/zulu-8.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn clean install -DskipTests
mkdir ~/Library/ParallelColt
cp ./target/parallelcolt-0.11.4.jar ~/Library/ParallelColt
```
- Lastly run the gradle build from the LuckyStackWorker project root folder:
```bash
gradle build
```

## Run

You can run the app from the command prompt like this:
```bash
gradle bootRun --args='--spring.profiles.active=mac --logging.level.root=INFO'
```
For windows, replace the spring.profiles.active=win, and for linux it is spring.profiles.active=linux.

For local development and debugging you can also run the app from you IDE's run configuration. Make sure to run the application's main class `nl.wilcokas.luckystackworker.LuckystackWorkerApplication` and specify the following VM options:

```
-Dspring.profiles.active=mac -Dlogging.level.root=INFO
```
