using Android.Graphics.Drawables;
using Bit.Droid.Effects;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportEffect(typeof(FabShadowEffect), "FabShadowEffect")]
namespace Bit.Droid.Effects
{
    public class FabShadowEffect : PlatformEffect
    {
        protected override void OnAttached ()
        {
            if (Control is Android.Widget.Button button)
            {
                var gd = new GradientDrawable();
                gd.SetColor(((Color)Application.Current.Resources["FabColor"]).ToAndroid());
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
