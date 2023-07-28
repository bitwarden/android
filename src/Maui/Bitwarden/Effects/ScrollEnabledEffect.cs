using Microsoft.Maui.Controls;
using Microsoft.Maui;

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
}
