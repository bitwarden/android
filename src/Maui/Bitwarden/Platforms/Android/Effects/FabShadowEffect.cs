using Android.Graphics.Drawables;
using Bit.App.Droid.Effects;
using Bit.App.Droid.Utilities;
using Microsoft.Maui.Controls.Platform;

[assembly: ExportEffect(typeof(FabShadowEffect), "FabShadowEffect")]
namespace Bit.App.Droid.Effects
{
    public class FabShadowEffect : PlatformEffect
    {
        protected override void OnAttached ()
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

        protected override void OnDetached ()
        {
        }
    }
}
