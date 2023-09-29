using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;

#if ANDROID
using Android.Graphics.Drawables;
using Bit.App.Droid.Utilities;
#endif

namespace Bit.App.Effects
{
#if ANDROID
    public class FabShadowPlatformEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Control is Android.Widget.Button button)
            {
                var gd = new GradientDrawable();
                gd.SetColor(ThemeHelpers.FabColor);
                gd.SetCornerRadius(100);

                button.SetBackground(gd);
                button.Elevation = 6;
                button.TranslationZ = 20;
            }
        }

        protected override void OnDetached()
        {
        }
    }
#endif
}
