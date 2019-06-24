using Bit.iOS.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(TabbedPage), typeof(CustomTabbedRenderer))]
namespace Bit.iOS.Renderers
{
    public class CustomTabbedRenderer : TabbedRenderer
    {
        protected override void OnElementChanged(VisualElementChangedEventArgs e)
        {
            base.OnElementChanged(e);
            TabBar.Translucent = false;
            TabBar.Opaque = true;
            TabBar.SelectedImageTintColor =
                ((Color)Xamarin.Forms.Application.Current.Resources["TabBarSelectedItemColor"]).ToUIColor();
            TabBar.UnselectedItemTintColor =
                ((Color)Xamarin.Forms.Application.Current.Resources["TabBarItemColor"]).ToUIColor();
        }
    }
}
