using System;
using Android.App;
using Android.Content;
using Java.Security;
using System.IO;
using Java.IO;
using Java.Security.Cert;
using Android.Util;
using Android.Content.PM;

namespace Bit.Android
{
    public static class Utilities
    {
        public static void SendCrashEmail(Exception e, bool includeSecurityProviders = true)
        {
            SendCrashEmail(e.Message + "\n\n" + e.StackTrace, includeSecurityProviders);
        }

        public static void SendCrashEmail(Activity act, Exception e, bool includeSecurityProviders = true)
        {
            SendCrashEmail(act, e.Message + "\n\n" + e.StackTrace, includeSecurityProviders);
        }

        public static void SaveCrashFile(Exception e, bool includeSecurityProviders = true)
        {
            SaveCrashFile(e.Message + "\n\n" + e.StackTrace, includeSecurityProviders);
        }

        public static void SendCrashEmail(string text, bool includeSecurityProviders = true)
        {
            var emailIntent = new Intent(Intent.ActionSend);

            emailIntent.SetType("plain/text");
            emailIntent.PutExtra(Intent.ExtraEmail, new String[] { "hello@bitwarden.com" });
            emailIntent.PutExtra(Intent.ExtraSubject, "bitwarden Crash Report");
            emailIntent.PutExtra(Intent.ExtraText, FormatText(text, includeSecurityProviders));

            Application.Context.StartActivity(Intent.CreateChooser(emailIntent, "Send mail..."));
        }

        public static void SendCrashEmail(Activity act, string text, bool includeSecurityProviders = true)
        {
            var emailIntent = new Intent(Intent.ActionSend);

            emailIntent.SetType("plain/text");
            emailIntent.PutExtra(Intent.ExtraEmail, new String[] { "hello@bitwarden.com" });
            emailIntent.PutExtra(Intent.ExtraSubject, "bitwarden Crash Report");
            emailIntent.PutExtra(Intent.ExtraText, FormatText(text, includeSecurityProviders));

            act.StartActivity(Intent.CreateChooser(emailIntent, "Send mail..."));
        }

        public static void SaveCrashFile(string text, bool includeSecurityProviders = true)
        {
            var path = Environment.GetFolderPath(Environment.SpecialFolder.Personal);
            var filename = Path.Combine(path, $"crash-{Java.Lang.JavaSystem.CurrentTimeMillis()}.txt");
            using(var streamWriter = new StreamWriter(filename, true))
            {
                streamWriter.WriteLine(FormatText(text, includeSecurityProviders));
            }
        }

        private static string FormatText(string text, bool includeSecurityProviders = true)
        {
            var crashMessage = "bitwarden has crashed. Please send this email to our support team so that we can help " +
                "resolve the problem for you. Thank you.";

            text = crashMessage + "\n\n =============================================== \n\n" + text;

            if(includeSecurityProviders)
            {
                text += "\n\n";
                var providers = Security.GetProviders();
                foreach(var provider in providers)
                {
                    text += ("provider: " + provider.Name + "\n");
                    var services = provider.Services;
                    foreach(var service in provider.Services)
                    {
                        text += ("- alg: " + service.Algorithm + "\n");
                    }
                }
            }

            text += "\n\n ==================================================== \n\n" + crashMessage;
            return text;
        }

        public static string AppendExceptionToMessage(string message, Exception ex)
        {
            message += ("\n\n" + ex.Message + "\n\n" + ex.StackTrace);
            if(ex.InnerException != null)
            {
                return AppendExceptionToMessage(message, ex.InnerException);
            }

            return message;
        }

        public static string GetFacetID(Application aContext, int callingUid)
        {
            var packageNames = aContext.PackageManager.GetPackagesForUid(callingUid);
            if(packageNames == null)
            {
                return null;
            }

            try
            {
                var info = aContext.PackageManager.GetPackageInfo(packageNames[0], PackageInfoFlags.Signatures);

                byte[] cert = info.Signatures[0].ToByteArray();
                var input = new MemoryStream(cert);

                var cf = CertificateFactory.GetInstance("X509");
                var c = (X509Certificate)cf.GenerateCertificate(input);

                var md = MessageDigest.GetInstance("SHA1");

                return "android:apk-key-hash:" + Base64.EncodeToString(md.Digest(c.GetEncoded()),
                          Base64Flags.Default | Base64Flags.NoPadding | Base64Flags.NoWrap);
            }
            catch(PackageManager.NameNotFoundException e)
            {
                e.PrintStackTrace();
            }
            catch(CertificateException e)
            {
                e.PrintStackTrace();
            }
            catch(NoSuchAlgorithmException e)
            {
                e.PrintStackTrace();
            }

            return null;
        }
    }
}