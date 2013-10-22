#!/bin/sh
ANDROID_PATH=/Applications/adt-bundle-mac-x86_64-20130219/sdk
#get lib
cp libs/armeabi-v7a/libmupdf.so bin

#compile to dalvik
cd bin
rm Temp.jar
rm classes.dex
rm mupdftool.jar
jar -cvf Temp.jar *
$ANDROID_PATH/build-tools/17.0.0/dx --dex --output=classes.dex Temp.jar
$ANDROID_PATH/build-tools/17.0.0/aapt add mupdftool.jar classes.dex
$ANDROID_PATH/build-tools/17.0.0/aapt add mupdftool.jar libmupdf.so
rm Temp.jar
rm classes.dex

#send to adb device
$ANDROID_PATH/platform-tools/adb push mupdftool.jar /sdcard/
$ANDROID_PATH/platform-tools/adb shell "cd /sdcard;\
/system/bin/mkdir /sdcard/dalvikfiles/dalvik-cache;\
export ANDROID_DATA=/sdcard/dalvikfiles;\
export CLASSPATH=/sdcard/mupdftool.jar;\
exec app_process –application com.nkey.mupdftool.Main"
