## Originator Compose App

Quickstart to run the sample screens (splash → login → home) shown in the mocks.

### Requirements
- JDK 17 (Temurin/Adoptium recommended). If you want a self-contained setup, keep the downloaded JDK in `.jdk` as below.
- Android SDK via Android Studio (or command-line tools) with API 36.

### Using the bundled JDK (created by this session)
If you keep the downloaded JDK at `.jdk/jdk-17.0.17+10/Contents/Home`, run:
```bash
cd /Users/yeswanthchowdary/Desktop/yesh/Kaleidofin/Originator
JAVA_HOME="$PWD/.jdk/jdk-17.0.17+10/Contents/Home" PATH="$JAVA_HOME/bin:$PATH" ./gradlew assembleDebug
```

### Using your own JDK
Install JDK 17 (e.g., Temurin), then export:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```
After that, build:
```bash
./gradlew assembleDebug
```

### Notes
- `.gitignore` excludes `.jdk/` so local JDK artifacts won’t be committed.
- The build uses AGP 8.13/Kotlin 2.0.21 and the Compose BOM 2024.09.00.



