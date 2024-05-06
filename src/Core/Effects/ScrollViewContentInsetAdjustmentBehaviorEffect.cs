using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;

#if IOS
using UIKit;
#endif

namespace Bit.App.Effects
{
    public enum ScrollContentInsetAdjustmentBehavior
    {
        Automatic,
        ScrollableAxes,
        Never,
        Always
    }

    public class ScrollViewContentInsetAdjustmentBehaviorEffect : RoutingEffect
    {
        public static readonly BindableProperty ContentInsetAdjustmentBehaviorProperty =
          BindableProperty.CreateAttached("ContentInsetAdjustmentBehavior", typeof(ScrollContentInsetAdjustmentBehavior), typeof(ScrollViewContentInsetAdjustmentBehaviorEffect), ScrollContentInsetAdjustmentBehavior.Automatic);

        public static ScrollContentInsetAdjustmentBehavior GetContentInsetAdjustmentBehavior(BindableObject view)
        {
            return (ScrollContentInsetAdjustmentBehavior)view.GetValue(ContentInsetAdjustmentBehaviorProperty);
        }

        public static void SetContentInsetAdjustmentBehavior(BindableObject view, ScrollContentInsetAdjustmentBehavior value)
        {
            view.SetValue(ContentInsetAdjustmentBehaviorProperty, value);
        }

        public ScrollViewContentInsetAdjustmentBehaviorEffect()
            : base($"Bitwarden.{nameof(ScrollViewContentInsetAdjustmentBehaviorEffect)}")
        {
        }
    }

#if IOS
    public class ScrollViewContentInsetAdjustmentBehaviorPlatformEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (Element != null && Control is UIScrollView scrollView)
            {
                switch (Bit.App.Effects.ScrollViewContentInsetAdjustmentBehaviorEffect.GetContentInsetAdjustmentBehavior(Element))
                {
                    case Bit.App.Effects.ScrollContentInsetAdjustmentBehavior.Automatic:
                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Automatic;
                        break;
                    case Bit.App.Effects.ScrollContentInsetAdjustmentBehavior.ScrollableAxes:
                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.ScrollableAxes;
                        break;
                    case Bit.App.Effects.ScrollContentInsetAdjustmentBehavior.Never:
                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Never;
                        break;
                    case Bit.App.Effects.ScrollContentInsetAdjustmentBehavior.Always:
                        scrollView.ContentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentBehavior.Always;
                        break;
                }
            }
        }

        protected override void OnDetached()
        {
        }
    }
#endif
}
