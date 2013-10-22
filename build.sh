#!/bin/sh
ANDROID_PATH=/Applications/adt-bundle-mac-x86_64-20130219/sdk/build-tools/17.0.0/

#get lib
cp libs/armeabi-v7a/libmupdf.so bin

#compile to dalvik
cd bin
rm Temp.jar
rm classes.dex
rm mupdftool.jar
jar -cvf Temp.jar *
$ANDROID_PATH/dx --dex --output=classes.dex Temp.jar
$ANDROID_PATH/aapt add mupdftool.jar classes.dex
rm Temp.jar
rm classes.dex
## convenience code. copy mupdftool.jar and libmupdf.so to your application folder, to run it with run_mupdftool.sh, as example project.
#cp mupdftool.jar LibTest/assets
#cp libmupdf.so LibTest/assets
echo "OK!"