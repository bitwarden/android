using Android.Graphics.Drawables;
using Android.Widget;
using Bit.Droid.Effects;
using Bit.Droid.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportEffect(typeof(RemoveFontPaddingEffect), "RemoveFontPaddingEffect")]
namespace Bit.Droid.Effects
{
    public class RemoveFontPaddingEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Element is Label label && Control is TextView textView)
            {
                textView.SetIncludeFontPadding(false);
            }
        }

        protected override void OnDetached()
        {
        }
    }
}
