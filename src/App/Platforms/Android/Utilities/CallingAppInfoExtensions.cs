using Android.OS;
using AndroidX.Credentials.Provider;
using Bit.Core.Utilities;
using Java.Security;

namespace Bit.App.Droid.Utilities
{
    public static class CallingAppInfoExtensions
    {
        public static string GetAndroidOrigin(this CallingAppInfo callingAppInfo)
        {
            if (Build.VERSION.SdkInt < BuildVersionCodes.P || callingAppInfo?.SigningInfo?.GetApkContentsSigners().Any() != true)
            {
                return null;
            }

            var cert = callingAppInfo.SigningInfo.GetApkContentsSigners()[0].ToByteArray();
            var md = MessageDigest.GetInstance("SHA-256");
            var certHash = md.Digest(cert);
            return $"android:apk-key-hash:{CoreHelpers.Base64UrlEncode(certHash)}";
        }
    }
}
