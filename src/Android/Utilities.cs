using System;

using Android.App;
using Android.Content;
using Java.Security;

namespace Bit.Android
{
    public static class Utilities
    {
        public static void SendCrashEmail(Exception e, bool includeSecurityProviders = true)
        {
            SendCrashEmail(e.Message + "\n\n" + e.StackTrace, includeSecurityProviders);
        }

        public static void SendCrashEmail(string text, bool includeSecurityProviders = true)
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

            var emailIntent = new Intent(Intent.ActionSend);

            emailIntent.SetType("plain/text");
            emailIntent.PutExtra(Intent.ExtraEmail, new String[] { "hello@bitwarden.com" });
            emailIntent.PutExtra(Intent.ExtraSubject, "bitwarden Crash Report");
            emailIntent.PutExtra(Intent.ExtraText, text);

            Application.Context.StartActivity(Intent.CreateChooser(emailIntent, "Send mail..."));
        }
    }
}