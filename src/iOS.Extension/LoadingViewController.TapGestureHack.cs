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
            ClipLogger.Log($"InitWithContext: {context?.UrlString}");
            _context = context;
            _shouldInitialize = false;
        }

        public void DismissLockAndContinue()
        {
            ClipLogger.Log("DismissLockAndContinue");
            if (UIApplication.SharedApplication.KeyWindow is null)
            {
                ClipLogger.Log("KeyWindow is null");
                return;
            }

            UIApplication.SharedApplication.KeyWindow.RootViewController = _storyboard.Value.InstantiateInitialViewController();

            if (UIApplication.SharedApplication.KeyWindow?.RootViewController is UINavigationController navContr)
            {
                var rootVC = navContr.ViewControllers.FirstOrDefault();
                if (rootVC is LoadingViewController loadingVC)
                {
                    ClipLogger.Log("Re-initing");
                    loadingVC.InitWithContext(_context);
                    loadingVC.ContinueOn();
                }
                else
                {
                    ClipLogger.Log($"Not LVC: {rootVC?.GetType().FullName}");
                }
            }
            else
            {
                ClipLogger.Log($"DismissLockAndContinue RVC not correct: {UIApplication.SharedApplication.KeyWindow?.RootViewController?.GetType().FullName}");
            }
            ClipLogger.Log("DismissLockAndContinue Done");
        }

        private void NavigateToPage(ContentPage page)
        {
            ClipLogger.Log($"NavigateToPage {page?.GetType().FullName}");
            var navigationPage = new NavigationPage(page);

            var window = new Window(navigationPage);
            window.ToHandler(MauiContextSingleton.Instance.MauiContext);
        }
    }
}

#endif