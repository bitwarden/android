using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;

#if IOS
using UIKit;
#endif

namespace Bit.App.Effects
{
    public class ScrollEnabledEffect : RoutingEffect
    {
        public static readonly BindableProperty IsScrollEnabledProperty =
          BindableProperty.CreateAttached("IsScrollEnabled", typeof(bool), typeof(ScrollEnabledEffect), true);

        public static bool GetIsScrollEnabled(BindableObject view)
        {
            return (bool)view.GetValue(IsScrollEnabledProperty);
        }

        public static void SetIsScrollEnabled(BindableObject view, bool value)
        {
            view.SetValue(IsScrollEnabledProperty, value);
        }

        public ScrollEnabledEffect()
            : base("Bitwarden.ScrollEnabledEffect")
        {
        }
    }

#if IOS
    public class ScrollEnabledPlatformEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            // this can be for any view that inherits from UIScrollView like UITextView.
            if (Element != null && Control is UIScrollView scrollView)
            {
                scrollView.ScrollEnabled = Bit.App.Effects.ScrollEnabledEffect.GetIsScrollEnabled(Element);
            }
        }

        protected override void OnDetached()
        {
        }
    }
#endif
}
