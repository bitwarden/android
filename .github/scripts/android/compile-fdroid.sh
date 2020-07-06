#!/usr/bin/env bash

cd $GITHUB_WORKSPACE
mkdir dist
cp CNAME ./dist
cd store
chmod 600 fdroid/config.py fdroid/keystore.jks
mkdir -p temp/fdroid
TEMP_DIR="$GITHUB_WORKSPACE/store/temp/fdroid"
cd fdroid
echo "keypass=\"$FDROID_STORE_KEYSTORE_PASSWORD\"" >>config.py
echo "keystorepass=\"$FDROID_STORE_KEYSTORE_PASSWORD\"" >>config.py
echo "local_copy_dir=\"$TEMP_DIR\"" >>config.py
mkdir -p repo
curl -Lo repo/com.x8bit.bitwarden-fdroid.apk \
https://github.com/bitwarden/mobile/releases/download/$RELEASE_TAG_NAME/com.x8bit.bitwarden-fdroid.apk
fdroid update
fdroid server update
cd ..
rm -rf temp/fdroid/archive
mv -v temp/fdroid ../dist
cd fdroid
cp index.html btn.png qr.png ../../dist/fdroid
cd $GITHUB_WORKSPACE
