#!/bin/bash
echo started
ls -l
set -exo pipefail
echo flutter home is $FLUTTER
#echo "1.21.0-8.0.pre.110" > $FLUTTER_ROOT/version
#ls -la $FLUTTER
VERSION=`cat $FLUTTER/version`
DVERSION=`cat $FLUTTER/bin/cache/dart-sdk/version`
echo Flutter version is $VERSION dart version is $DVERSION
echo FLUTTER: Cleaning up after last build

cd app_mr_layer && flutter pub get
echo "Flutter App will be version: ${BUILD_VERSION}"
cd ../open_admin_app && echo "final appVersion = '${BUILD_VERSION}';" > lib/version.dart && flutter clean && flutter pub get

rename_main_dart() {
  cd build/web
  MAIN_SHA=`sha256sum main.dart.js | awk '{print $1}'`
  echo Generated SHA is $MAIN_SHA
  MAIN="main-$MAIN_SHA.js"
  if [[ "$OSTYPE" == 'darwin'* ]]; then
    gsed -i s/main.dart.js/$MAIN/ index.html
  else
    sed -i s/main.dart.js/$MAIN/ index.html
  fi
  mv main.dart.js $MAIN
  if test -f "main.dart.js.map"; then
    mv main.dart.js.map $MAIN.map
  fi
  cd ../..
}

echo FLUTTER: building deploy_main
#flutter analyze
#if test "$?" != "0"; then
#  echo "failed"
#  exit 1
#fi
echo "building normal version"
flutter build web --target=lib/deploy_main.dart --release --no-tree-shake-icons

rename_main_dart
mv build build_original
mkdir -p build/web/assets

echo "building canvaskit embedded version"
# Flutter already downloads Canvaskit, this just lets us use what it has already downloaded
flutter build web --dart-define=FLUTTER_WEB_CANVASKIT_URL=canvaskit/ --target=lib/deploy_main.dart  --release --no-tree-shake-icons

rename_main_dart

mv build/web build_original/web/intranet
rm -rf build

echo "building html renderer version"
flutter build web --target=lib/deploy_main.dart --web-renderer html  --release --no-tree-shake-icons

mv build/web build_original/web/html
rm -rf build

mv build_original build

echo FLUTTER: finished building, cleaning
exit 0
