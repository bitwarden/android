using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;

namespace Bit.Android
{
    //[Activity(LaunchMode = global::Android.Content.PM.LaunchMode.SingleInstance)]
    public class AutofillSelectLoginActivity : Activity
    {
        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);
            var url = Intent.GetStringExtra("url");

            Intent data = new Intent();
            data.PutExtra("url", url);
            data.PutExtra("username", "user123");
            data.PutExtra("password", "pass123");

            if(Parent == null)
            {
                SetResult(Result.Ok, data);
            }
            else
            {
                Parent.SetResult(Result.Ok, data);
            }

            Finish();
        }
    }
}