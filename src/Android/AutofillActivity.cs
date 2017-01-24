using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Bit.App.Models;

namespace Bit.Android
{
    //[Activity(Label = "Autofill", LaunchMode = global::Android.Content.PM.LaunchMode.SingleInstance, Theme = "@style/android:Theme.Material.Light")]
    public class AutofillActivity : Activity
    {
        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);

            var url = Intent.GetStringExtra("url");
            _lastQueriedUrl = url;

            Intent intent = new Intent(this, typeof(AutofillSelectLoginActivity));
            intent.PutExtra("url", url);
            StartActivityForResult(intent, 123);
        }

        string _lastQueriedUrl;

        protected override void OnActivityResult(int requestCode, [GeneratedEnum] Result resultCode, Intent data)
        {
            var url = data.GetStringExtra("url");
            var username = data.GetStringExtra("username");
            var password = data.GetStringExtra("password");

            base.OnActivityResult(requestCode, resultCode, data);

            try
            {
                LastReceivedCredentials = new Credentials { User = username, Password = password, Url = _lastQueriedUrl };
            }
            catch(Exception e)
            {
                //Android.Util.Log.Debug("KP2AAS", "Exception while receiving credentials: " + e.ToString());
            }
            finally
            {
                Finish();
            }
        }

        public static Credentials LastReceivedCredentials;

        public class Credentials
        {
            public string User;
            public string Password;
            public string Url;
        }
    }
}