#if ENABLED_TAP_GESTURE_RECOGNIZER_MAUI_EMBEDDED_WORKAROUND

using System;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost
    {
        const string STORYBOARD_NAME = "MainInterface";
        Lazy<UIStoryboard> _storyboard = new Lazy<UIStoryboard>(() => UIStoryboard.FromName(STORYBOARD_NAME, null));

        public void InitWithContext(Context context)
        {
            _context = context;
        }

        public void DismissLockAndContinue()
        {
            if (UIApplication.SharedApplication.KeyWindow is null)
            {
                return;
            }

            UIApplication.SharedApplication.KeyWindow.RootViewController = _storyboard.Value.InstantiateInitialViewController();

            if (UIApplication.SharedApplication.KeyWindow?.RootViewController is CredentialProviderViewController cpvc)
            {
                cpvc.InitWithContext(_context);
                cpvc.OnLockDismissedAsync().FireAndForget();
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
