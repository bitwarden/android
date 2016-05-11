using System;
using Bit.App.Controls;
using Bit.iOS.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedTabbedPage), typeof(ExtendedTabbedPageRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedTabbedPageRenderer : TabbedRenderer
    {
        protected override void OnElementChanged(VisualElementChangedEventArgs e)
        {
            base.OnElementChanged(e);

            var page = (ExtendedTabbedPage)Element;

            TabBar.TintColor = page.TintColor.ToUIColor();
            TabBar.BarTintColor = page.BarTintColor.ToUIColor();
            TabBar.BackgroundColor = page.BackgroundColor.ToUIColor();
        }
    }
}
