using System;
using System.ComponentModel;
using Android.Graphics.Drawables;
using Bit.Droid.Effects;
using Bit.Droid.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportEffect(typeof(HintColorSwitchEffect), "HintColorSwitchEffect")]
namespace Bit.Droid.Effects
{
    public class HintColorSwitchEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Control is Android.Widget.Switch switchView)
            {
                switchView.SetHintTextColor(ThemeHelpers.MutedColor);
            }
        }

        protected override void OnDetached()
        {
        }
    }
}

