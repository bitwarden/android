#if ENABLED_TAP_GESTURE_RECOGNIZER_MAUI_EMBEDDED_WORKAROUND

using Bit.iOS.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.ShareExtension
{
    public partial class LoadingViewController : UIViewController
    {
        private void NavigateToPage(ContentPage page)
        {
            var navigationPage = new NavigationPage(page);

            var window = new Window(navigationPage);
            window.ToHandler(MauiContextSingleton.Instance.MauiContext);
        }
    }
}

#endif
