using Android.Widget;
using Bit.App.Droid.Effects;
using Microsoft.Maui.Controls.Platform;

[assembly: ExportEffect(typeof(FixedSizeEffect), "FixedSizeEffect")]
namespace Bit.App.Droid.Effects
{
    public class FixedSizeEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Element is Label label && Control is TextView textView)
            {
                textView.SetTextSize(Android.Util.ComplexUnitType.Pt, (float)label.FontSize);
            }
        }

        protected override void OnDetached()
        {
        }
    }
}
