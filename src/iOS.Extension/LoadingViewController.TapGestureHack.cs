#if ENABLED_TAP_GESTURE_RECOGNIZER_MAUI_EMBEDDED_WORKAROUND

using System;
using System.Linq;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Extension.Models;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Extension
{
    public partial class LoadingViewController : UIViewController
    {
        const string STORYBOARD_NAME = "MainInterface";
        Lazy<UIStoryboard> _storyboard = new Lazy<UIStoryboard>(() => UIStoryboard.FromName(STORYBOARD_NAME, null));

        public void InitWithContext(Context context)
        {
            _context = context;
            _shouldInitialize = false;
        }

        public void DismissLockAndContinue()
        {
            if (UIApplication.SharedApplication.KeyWindow is null)
            {
                return;
            }

            UIApplication.SharedApplication.KeyWindow.RootViewController = _storyboard.Value.InstantiateInitialViewController();

            if (UIApplication.SharedApplication.KeyWindow?.RootViewController is UINavigationController navContr)
            {
                var rootVC = navContr.ViewControllers.FirstOrDefault();
                if (rootVC is LoadingViewController loadingVC)
                {
                    loadingVC.InitWithContext(_context);
                    loadingVC.ContinueOn();
                }
            }
        }

        private void NavigateToPage(ContentPage page)
        {
            var navigationPage = new NavigationPage(page);

            var window = new Window(navigationPage);
            window.ToHandler(MauiContextSingleton.Instance.MauiContext);
        }
    }
}

#endif