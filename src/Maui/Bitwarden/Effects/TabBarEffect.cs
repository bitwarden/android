using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Platform;

namespace Bit.App.Effects
{
    public class TabBarEffect : RoutingEffect
    {
    }

#if ANDROID
    public class TabBarPlatformEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            // TODO: [MAUI-Migration] [Critical]
            // now Container is View instead of ViewGroup, let's review this
            //if (!(Container.GetChildAt(0) is ViewGroup layout))
            //{
            //    return;
            //}
            //if (!(layout.GetChildAt(1) is BottomNavigationView bottomNavigationView))
            //{
            //    return;
            //}
            //bottomNavigationView.LabelVisibilityMode = LabelVisibilityMode.LabelVisibilityLabeled;
        }

        protected override void OnDetached()
        {
        }
    }
#endif
}
