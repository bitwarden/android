#! /bin/sh

function print_example() {
    echo "Example"
    echo "  icons ios ~/AppIcon.pdf ~/Icons/"
}
    
function print_usage() {
    echo "Usage"
    echo "  icons <ios|watch|complication|macos> in-file.pdf (out-dir)"
}

function command_exists() {
    if type "$1" >/dev/null 2>&1; then
        return 1
    else
        return 0
    fi
}

if command_exists "sips" == 0 ; then
    echo "sips tool not found"
    exit 1
fi

if [ "$1" = "--help" ] || [ "$1" = "-h" ] ; then
    print_usage
    exit 0
fi

PLATFORM="$1"
FILE="$2"
if [ -z "$PLATFORM" ] || [ -z "$FILE" ] ; then
    echo "Error: missing arguments"
    echo ""
    print_usage
    echo ""
    print_example
    exit 1
fi

DIR="$3"
if [ -z "$DIR" ] ; then
    DIR=$(dirname $FILE)
fi

# Create directory if needed
mkdir -p "$DIR"

if [[ "$PLATFORM" == *"ios"* ]] ; then # iOS
    sips -s format png -Z '180'  "${FILE}" --out "${DIR}"/Icon-180.png
    sips -s format png -Z '29'   "${FILE}" --out "${DIR}"/Icon-29.png
    sips -s format png -Z '58'   "${FILE}" --out "${DIR}"/Icon-58.png
    sips -s format png -Z '120'  "${FILE}" --out "${DIR}"/Icon-120.png
    sips -s format png -Z '87'   "${FILE}" --out "${DIR}"/Icon-87.png
    sips -s format png -Z '40'   "${FILE}" --out "${DIR}"/Icon-40.png
    sips -s format png -Z '80'   "${FILE}" --out "${DIR}"/Icon-80.png
    sips -s format png -Z '76'   "${FILE}" --out "${DIR}"/Icon-76.png
    sips -s format png -Z '152'  "${FILE}" --out "${DIR}"/Icon-152.png
    sips -s format png -Z '167'  "${FILE}" --out "${DIR}"/Icon-167.png
    sips -s format png -Z '60'   "${FILE}" --out "${DIR}"/Icon-60.png
    sips -s format png -Z '20'   "${FILE}" --out "${DIR}"/Icon-20.png
    sips -s format png -Z '1024' "${FILE}" --out "${DIR}"/Icon-1024.png
    
    # https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/AppIconType.html
    contents_json='{"images":[{"size":"20x20","idiom":"iphone","filename":"iPhoneNotification@2x.png","scale":"2x"},{"size":"20x20","idiom":"iphone","filename":"iPhoneNotification@3x.png","scale":"3x"},{"size":"29x29","idiom":"iphone","filename":"iPhoneSettings@2x.png","scale":"2x"},{"size":"29x29","idiom":"iphone","filename":"iPhoneSettings@3x.png","scale":"3x"},{"size":"40x40","idiom":"iphone","filename":"iPhoneSpotlight@2x.png","scale":"2x"},{"size":"40x40","idiom":"iphone","filename":"iPhoneSpotlight@3x.png","scale":"3x"},{"size":"60x60","idiom":"iphone","filename":"iPhone@2x.png","scale":"2x"},{"size":"60x60","idiom":"iphone","filename":"iPhone@3x.png","scale":"3x"},{"size":"20x20","idiom":"ipad","filename":"iPadNotification.png","scale":"1x"},{"size":"20x20","idiom":"ipad","filename":"iPadNotification@2x.png","scale":"2x"},{"size":"29x29","idiom":"ipad","filename":"iPadSettings.png","scale":"1x"},{"size":"29x29","idiom":"ipad","filename":"iPadSettings@2x.png","scale":"2x"},{"size":"40x40","idiom":"ipad","filename":"iPadSpotlight.png","scale":"1x"},{"size":"40x40","idiom":"ipad","filename":"iPadSpotlight@2x.png","scale":"2x"},{"size":"76x76","idiom":"ipad","filename":"iPad.png","scale":"1x"},{"size":"76x76","idiom":"ipad","filename":"iPad@2x.png","scale":"2x"},{"size":"83.5x83.5","idiom":"ipad","filename":"iPadPro@2x.png","scale":"2x"},{"size":"1024x1024","idiom":"ios-marketing","filename":"AppStoreMarketing.png","scale":"1x"}],"info":{"version":1,"author":"xcode"}}'
    echo $contents_json > "${DIR}"/Contents.json
fi

if [[ "$PLATFORM" == *"watch"* ]] ; then # Apple Watch
    sips -s format png -Z '48'  "${FILE}" --out "${DIR}"/Watch38mmNotificationCenter.png
    sips -s format png -Z '55'  "${FILE}" --out "${DIR}"/Watch42mmNotificationCenter.png
    sips -s format png -Z '66'  "${FILE}" --out "${DIR}"/Watch66NotificationCenter.png
    sips -s format png -Z '58'  "${FILE}" --out "${DIR}"/WatchCompanionSettings@2x.png
    sips -s format png -Z '87'  "${FILE}" --out "${DIR}"/WatchCompanionSettings@3x.png
    sips -s format png -Z '80'  "${FILE}" --out "${DIR}"/Watch38MM42MMHomeScreen.png
    sips -s format png -Z '88'  "${FILE}" --out "${DIR}"/Watch40MMHomeScreen.png
    sips -s format png -Z '92' "${FILE}" --out "${DIR}"/Watch41MMHomeScreen.png
    sips -s format png -Z '100' "${FILE}" --out "${DIR}"/Watch44MMHomeScreen.png
    sips -s format png -Z '102' "${FILE}" --out "${DIR}"/Watch45MMHomeScreen.png
    sips -s format png -Z '108' "${FILE}" --out "${DIR}"/Watch49MMHomeScreen.png
    sips -s format png -Z '172' "${FILE}" --out "${DIR}"/Watch38MMShortLook.png
    sips -s format png -Z '196' "${FILE}" --out "${DIR}"/Watch40MM42MMShortLook.png
    sips -s format png -Z '216' "${FILE}" --out "${DIR}"/Watch44MMShortLook.png
    sips -s format png -Z '234' "${FILE}" --out "${DIR}"/Watch234ShortLook.png
    sips -s format png -Z '258' "${FILE}" --out "${DIR}"/Watch258ShortLook.png
    sips -s format png -Z '1024' "${FILE}" --out "${DIR}"/WatchAppStore.png

    # https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/AppIconType.html
    contents_json='{"images":[{"size":"24x24","idiom":"watch","scale":"2x","filename":"Watch38mmNotificationCenter.png","role":"notificationCenter","subtype":"38mm"},{"size":"27.5x27.5","idiom":"watch","scale":"2x","filename":"Watch42mmNotificationCenter.png","role":"notificationCenter","subtype":"42mm"},{"size":"29x29","idiom":"watch","filename":"WatchCompanionSettings@2x.png","role":"companionSettings","scale":"2x"},{"size":"29x29","idiom":"watch","filename":"WatchCompanionSettings@3x.png","role":"companionSettings","scale":"3x"},{"size":"40x40","idiom":"watch","filename":"Watch38MM42MMHomeScreen.png","scale":"2x","role":"appLauncher","subtype":"38mm"},{"size":"44x44","idiom":"watch","scale":"2x","filename":"Watch40MMHomeScreen.png","role":"appLauncher","subtype":"40mm"},{"size":"50x50","idiom":"watch","scale":"2x","filename":"Watch44MMHomeScreen.png","role":"appLauncher","subtype":"44mm"},{"size":"86x86","idiom":"watch","scale":"2x","filename":"Watch38MMShortLook.png","role":"quickLook","subtype":"38mm"},{"size":"98x98","idiom":"watch","scale":"2x","filename":"Watch40MM42MMShortLook.png","role":"quickLook","subtype":"42mm"},{"size":"108x108","idiom":"watch","scale":"2x","filename":"Watch44MMShortLook.png","role":"quickLook","subtype":"44mm"},{"idiom":"watch-marketing","filename":"WatchAppStore.png","size":"1024x1024","scale":"1x"}],"info":{"version":1,"author":"xcode"}}'
    echo $contents_json > "${DIR}"/Contents.json
fi

if [[ "$PLATFORM" == *"complication"* ]] ; then # Apple Watch
    sips -s format png -Z '32'  "${FILE}" --out "${DIR}"/Circular38mm2x.png
    sips -s format png -Z '36'  "${FILE}" --out "${DIR}"/Circular40mm2x.png
    sips -s format png -Z '36'  "${FILE}" --out "${DIR}"/Circular42mm2x.png
    sips -s format png -Z '40'  "${FILE}" --out "${DIR}"/Circular44mm2x.png
    sips -s format png -Z '182'  "${FILE}" --out "${DIR}"/ExtraLarge38mm2x.png
    sips -s format png -Z '203'  "${FILE}" --out "${DIR}"/ExtraLarge40mm2x.png
    sips -s format png -Z '203'  "${FILE}" --out "${DIR}"/ExtraLarge42mm2x.png
    sips -s format png -Z '224'  "${FILE}" --out "${DIR}"/ExtraLarge44mm2x.png
    sips -s format png -Z '84'  "${FILE}" --out "${DIR}"/GraphicBezel40mm2x.png
    sips -s format png -Z '84'  "${FILE}" --out "${DIR}"/GraphicBezel42mm2x.png
    sips -s format png -Z '94'  "${FILE}" --out "${DIR}"/GraphicBezel44mm2x.png
    sips -s format png -Z '84'  "${FILE}" --out "${DIR}"/GraphicCircular40mm2x.png
    sips -s format png -Z '84'  "${FILE}" --out "${DIR}"/GraphicCircular42mm2x.png
    sips -s format png -Z '94'  "${FILE}" --out "${DIR}"/GraphicCircular44mm2x.png
    sips -s format png -Z '40'  "${FILE}" --out "${DIR}"/GraphicCorner40mm2x.png
    sips -s format png -Z '40'  "${FILE}" --out "${DIR}"/GraphicCorner42mm2x.png
    sips -s format png -Z '44'  "${FILE}" --out "${DIR}"/GraphicCorner44mm2x.png
    sips -s format png -Z '52'  "${FILE}" --out "${DIR}"/GraphicModular38mm2x.png
    sips -s format png -Z '58'  "${FILE}" --out "${DIR}"/GraphicModular40mm2x.png
    sips -s format png -Z '58'  "${FILE}" --out "${DIR}"/GraphicModular42mm2x.png
    sips -s format png -Z '64'  "${FILE}" --out "${DIR}"/GraphicModular44mm2x.png
    sips -s format png -Z '40'  "${FILE}" --out "${DIR}"/GraphicUtilitarian38mm2x.png
    sips -s format png -Z '44'  "${FILE}" --out "${DIR}"/GraphicUtilitarian40mm2x.png
    sips -s format png -Z '44'  "${FILE}" --out "${DIR}"/GraphicUtilitarian42mm2x.png
    sips -s format png -Z '50'  "${FILE}" --out "${DIR}"/GraphicUtilitarian44mm2x.png
    sips -s format png -Z '206'  "${FILE}" --out "${DIR}"/GraphicExtraLarge38mm2x.png
    sips -s format png -Z '264'  "${FILE}" --out "${DIR}"/GraphicExtraLarge44mm2x.png
    echo "NOTE: Graphic Extra Large is not generated since that is not rectangular"
fi

if [[ "$PLATFORM" == *"macos"* ]] ; then # macOS
    sips -s format png -Z '1024' "${FILE}" --out "${DIR}"/icon_512x512@2x.png
    sips -s format png -Z '512'  "${FILE}" --out "${DIR}"/icon_512x512.png
    sips -s format png -Z '512'  "${FILE}" --out "${DIR}"/icon_256x256@2x.png
    sips -s format png -Z '256'  "${FILE}" --out "${DIR}"/icon_256x256.png
    sips -s format png -Z '256'  "${FILE}" --out "${DIR}"/icon_128x128@2x.png
    sips -s format png -Z '128'  "${FILE}" --out "${DIR}"/icon_128x128.png
    sips -s format png -Z '64'   "${FILE}" --out "${DIR}"/icon_32x32@2x.png
    sips -s format png -Z '32'   "${FILE}" --out "${DIR}"/icon_32x32.png
    sips -s format png -Z '32'   "${FILE}" --out "${DIR}"/icon_16x16@2x.png
    sips -s format png -Z '16'   "${FILE}" --out "${DIR}"/icon_16x16.png
fi