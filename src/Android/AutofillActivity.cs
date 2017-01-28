using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;

namespace Bit.Android
{
    [Activity(Label = "bitwarden",
        Icon = "@drawable/icon",
        LaunchMode = global::Android.Content.PM.LaunchMode.SingleInstance,
        WindowSoftInputMode = SoftInput.StateHidden)]
    public class AutofillActivity : Activity
    {
        private string _lastQueriedUri;
        public static Credentials LastCredentials;

        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);
            _lastQueriedUri = Intent.GetStringExtra("uri");

            var intent = new Intent(this, typeof(MainActivity));
            intent.PutExtra("uri", _lastQueriedUri);
            StartActivityForResult(intent, 123);
        }

        protected override void OnActivityResult(int requestCode, [GeneratedEnum] Result resultCode, Intent data)
        {
            base.OnActivityResult(requestCode, resultCode, data);

            try
            {
                var uri = data.GetStringExtra("uri");
                var username = data.GetStringExtra("username");
                var password = data.GetStringExtra("password");

                LastCredentials = new Credentials
                {
                    User = username,
                    Password = password,
                    Uri = _lastQueriedUri
                };
            }
            catch { }
            finally
            {
                Finish();
            }
        }

        public class Credentials
        {
            public string User;
            public string Password;
            public string Uri;
        }
    }
}