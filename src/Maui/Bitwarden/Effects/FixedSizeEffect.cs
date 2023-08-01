using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;

#if ANDROID
using Android.Widget;
#endif


namespace Bit.App.Effects
{
    public class FixedSizeEffect : RoutingEffect
    {
    }

#if ANDROID
    public class FixedSizePlatformEffect : PlatformEffect
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
#endif
}
