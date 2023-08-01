using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;
#if ANDROID
using Android.Widget;
#endif

namespace Bit.App.Effects
{
    public class RemoveFontPaddingEffect : RoutingEffect
    {
    }

#if ANDROID
    public class RemoveFontPaddingPlatformEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Control is TextView textView)
            {
                textView.SetIncludeFontPadding(false);
            }
        }

        protected override void OnDetached()
        {
        }
    }
#endif
}

