using Bit.iOS.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(TabbedPage), typeof(CustomTabbedRenderer))]
namespace Bit.iOS.Renderers
{
    public class CustomTabbedRenderer : TabbedRenderer
    {
        public CustomTabbedRenderer()
        {
            TabBar.Translucent = false;
            TabBar.Opaque = true;
        }
    }
}
