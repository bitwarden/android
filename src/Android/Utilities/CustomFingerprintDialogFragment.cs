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
using Plugin.Fingerprint.Dialog;
using Xamarin.Forms.Platform.Android;

namespace Bit.Droid.Utilities
{
    public class CustomFingerprintDialogFragment : FingerprintDialogFragment
    {
        public CustomFingerprintDialogFragment()
            : base()
        {
            DefaultColor = ((Xamarin.Forms.Color)Xamarin.Forms.Application.Current.Resources["MutedColor"])
                .ToAndroid();
            NegativeColor = ((Xamarin.Forms.Color)Xamarin.Forms.Application.Current.Resources["DangerColor"])
                .ToAndroid();
            PositiveColor = ((Xamarin.Forms.Color)Xamarin.Forms.Application.Current.Resources["SuccessColor"])
                .ToAndroid();
        }
    }
}