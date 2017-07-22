using System;
using Android.App;
using Android.Content;
using Java.Security;
using System.IO;
using Android.Nfc;
using Android.Provider;

namespace Bit.Android
{
    public static class Utilities
    {
        public static bool NfcEnabled()
        {
            var manager = (NfcManager)Application.Context.GetSystemService("nfc");
            var adapter = manager.DefaultAdapter;
            return adapter != null && adapter.IsEnabled;
        }

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

        public static string GetFileName(Context context, global::Android.Net.Uri uri)
        {
            string name = null;
            string[] projection = { MediaStore.MediaColumns.DisplayName };
            var metaCursor = context.ContentResolver.Query(uri, projection, null, null, null);
            if(metaCursor != null)
            {
                try
                {
                    if(metaCursor.MoveToFirst())
                    {
                        name = metaCursor.GetString(0);
                    }
                }
                finally
                {
                    metaCursor.Close();
                }
            }

            return name;
        }
    }
}