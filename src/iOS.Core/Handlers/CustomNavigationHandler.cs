using Bit.App.Controls;
using CoreFoundation;
using System.ComponentModel;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    //This is a Compatibility verion of the NavigationRenderer. Eventually we should see if there's a better way to implement this behavior.
    public class CustomNavigationHandler : Microsoft.Maui.Controls.Handlers.Compatibility.NavigationRenderer
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

            // In order to get the correct index we need to do the same as XF and reverse the toolbar items list
            // https://github.com/xamarin/Xamarin.Forms/blob/8f765bd87a2968bef9c86122d88c9c47be9196d2/Xamarin.Forms.Platform.iOS/Renderers/NavigationRenderer.cs#L1432
            toolbarItems = toolbarItems.Where(t => t.Order != ToolbarItemOrder.Secondary)
                                       .Reverse()
                                       .ToList();

            var uiBarButtonItems = TopViewController.NavigationItem.RightBarButtonItems;
            if (uiBarButtonItems == null)
            {
                return;
            }

            foreach (ExtendedToolbarItem toolbarItem in toolbarItems.Where(t => t is ExtendedToolbarItem eti
                                                                &&
                                                                eti.UseOriginalImage))
            {
                var index = toolbarItems.IndexOf(toolbarItem);
                if (index < 0 || index >= uiBarButtonItems.Length)
                {
                    continue;
                }

                // HACK: this is awful but I can't find another way to properly prevent memory leaks from
                // subscribing on the PropertyChanged event; there are several private places where Xamarin Forms
                // disposes objects that are not accessible from here so I think this should cover the (un)subscription
                // but we need to remember to call the internal methods of ExtendedToolbarItem on the lifecycle of the Page
                toolbarItem.OnAppearingAction = () => toolbarItem.PropertyChanged += ToolbarItem_PropertyChanged;
                toolbarItem.OnDisappearingAction = () => toolbarItem.PropertyChanged -= ToolbarItem_PropertyChanged;

                // HACK: XF PimaryToolbarItem is sealed so we can't override it, and also it doesn't provide any
                // direct way to replace it with our custom one (we can but we need to rewrite several parts of the NavigationRenderer)
                // So I think this is the easiest soolution for now to set UIImageRenderingMode.AlwaysOriginal
                // on the toolbar item image
                void ToolbarItem_PropertyChanged(object s, PropertyChangedEventArgs e)
                {
                    if (e.PropertyName == nameof(ExtendedToolbarItem.IconImageSource))
                    {
                        var uiBarButtonItem = uiBarButtonItems[index];

                        DispatchQueue.MainQueue.DispatchAsync(() =>
                        {
                            try
                            {
                                uiBarButtonItem.Image = uiBarButtonItem.Image?.ImageWithRenderingMode(UIImageRenderingMode.AlwaysOriginal);
                            }
                            catch (ObjectDisposedException)
                            {
                                // Do nothing, we can't access the proper place to properly dispose this, so here
                                // we can just catch and ignore the exception. This should only happen when logging out a user.
                            }
                        });
                    }
                };
            }
        }
    }
}