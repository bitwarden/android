using Android.Views;
using Bit.Droid.Effects;
using Google.Android.Material.BottomNavigation;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ResolutionGroupName("Bitwarden")]
[assembly: ExportEffect(typeof(TabBarEffect), "TabBarEffect")]
namespace Bit.Droid.Effects
{
    public class TabBarEffect : PlatformEffect
    {
        protected override void OnAttached()
        {
            if (!(Container.GetChildAt(0) is ViewGroup layout))
            {
                return;
            }
            if (!(layout.GetChildAt(1) is BottomNavigationView bottomNavigationView))
            {
                return;
            }
            bottomNavigationView.LabelVisibilityMode = LabelVisibilityMode.LabelVisibilityLabeled;
        }

        protected override void OnDetached()
        {
        }
    }
}
