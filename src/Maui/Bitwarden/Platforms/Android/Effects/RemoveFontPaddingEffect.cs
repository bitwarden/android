using Android.Widget;
using Bit.App.Droid.Effects;
using Microsoft.Maui.Controls.Platform;

[assembly: ExportEffect(typeof(RemoveFontPaddingEffect), nameof(RemoveFontPaddingEffect))]
namespace Bit.App.Droid.Effects
{
    public class RemoveFontPaddingEffect : PlatformEffect
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
}