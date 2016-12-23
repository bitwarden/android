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
            //StartActivityForResult(Kp2aControl.GetQueryEntryIntent(url), 123);
        }

        string _lastQueriedUrl;

        protected override void OnActivityResult(int requestCode, [GeneratedEnum] Result resultCode, Intent data)
        {
            base.OnActivityResult(requestCode, resultCode, data);

            try
            {
                // TODO: lookup site
                LastReceivedCredentials = new Credentials { User = "username", Password = "12345678", Url = _lastQueriedUrl };
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