using Microsoft.Maui.Controls;
using Microsoft.Maui;

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
}
