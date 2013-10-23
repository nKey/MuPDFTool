#!/bin/sh

echo "SH: Preparing to execute mupdftool"
export APP_PATH=$1
/system/bin/mkdir $APP_PATH/dalvik-cache
export ANDROID_DATA=$APP_PATH
export CLASSPATH=$APP_PATH/mupdftool.jar
/system/bin/chmod 777 -R $APP_PATH
echo "SH: Starting app_process mupdftool"
exec app_process –application com.nkey.mupdftool.Main -lib $1 $2 $3 $4 $5 $6 $7 $8 $9
echo "SH: MuPDFTool finished"