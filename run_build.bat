@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
cd /d "F:\Code\android_app\ieeeconnect"
echo JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java" -version
call gradlew.bat :app:compileDebugJavaWithJavac --console=plain


