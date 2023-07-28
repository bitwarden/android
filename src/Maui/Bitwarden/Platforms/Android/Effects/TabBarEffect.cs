using Android.Views;
using Bit.App.Droid.Effects;
using Google.Android.Material.BottomNavigation;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.Platform;

[assembly: ResolutionGroupName("Bitwarden")]
[assembly: ExportEffect(typeof(TabBarEffect), "TabBarEffect")]
namespace Bit.App.Droid.Effects
{
    public class TabBarEffect : PlatformEffect
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
}
