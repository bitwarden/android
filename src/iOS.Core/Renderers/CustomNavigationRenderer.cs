using System.Linq;
using Bit.App.Controls;
using Bit.iOS.Core.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(NavigationPage), typeof(CustomNavigationRenderer))]
namespace Bit.iOS.Core.Renderers
{
    public class CustomNavigationRenderer : NavigationRenderer
    {
        public override void PushViewController(UIViewController viewController, bool animated)
        {
            base.PushViewController(viewController, animated);

            var currentPage = (Element as NavigationPage)?.CurrentPage;
            if (currentPage == null)
            {
                return;
            }
            var toolbarItems = currentPage.ToolbarItems;
            if (!toolbarItems.Any())
            {
                return;
            }
            var uiBarButtonItems = TopViewController.NavigationItem.RightBarButtonItems;
            if (uiBarButtonItems == null)
            {
                return;
            }

            foreach (var toolbarItem in toolbarItems)
            {
                if (!(toolbarItem is ExtendedToolbarItem extendedToolbarItem) || !extendedToolbarItem.UseOriginalImage)
                {
                    continue;
                }
                var index = currentPage.ToolbarItems.IndexOf(extendedToolbarItem) + 1;
                if (index < 0 || index >= uiBarButtonItems.Length)
                {
                    continue;
                }
                var uiBarButtonItem = uiBarButtonItems[index];
                if (uiBarButtonItem.Image == null)
                {
                    continue;
                }
                var originalImage = uiBarButtonItem.Image.ImageWithRenderingMode(UIImageRenderingMode.AlwaysOriginal);
                uiBarButtonItem.Image = originalImage;
            }
        }
    }
}
