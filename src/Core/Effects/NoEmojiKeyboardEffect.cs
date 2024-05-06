using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;

#if ANDROID
using Android.Widget;
#endif

namespace Bit.App.Effects
{
    public class NoEmojiKeyboardEffect : RoutingEffect
    {
    }

#if ANDROID
    public class NoEmojiKeyboardPlatformEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Control is EditText editText)
            {
                editText.InputType = Android.Text.InputTypes.ClassText | Android.Text.InputTypes.TextVariationVisiblePassword | Android.Text.InputTypes.TextFlagMultiLine;
            }
        }

        protected override void OnDetached()
        {
        }
    }
#endif
}
