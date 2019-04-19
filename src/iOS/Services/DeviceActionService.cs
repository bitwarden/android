using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Enums;
using Bit.iOS.Core.Views;
using CoreGraphics;
using Foundation;
using UIKit;

namespace Bit.iOS.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private Toast _toast;
        private UIAlertController _progressAlert;

        public DeviceType DeviceType => DeviceType.iOS;

        public bool LaunchApp(string appName)
        {
            throw new NotImplementedException();
        }

        public void Toast(string text, bool longDuration = false)
        {
            if(!_toast?.Dismissed ?? false)
            {
                _toast.Dismiss(false);
            }
            _toast = new Toast(text)
            {
                Duration = TimeSpan.FromSeconds(longDuration ? 5 : 3)
            };
            if(TabBarVisible())
            {
                _toast.BottomMargin = 55;
            }
            _toast.Show();
            _toast.DismissCallback = () =>
            {
                _toast?.Dispose();
                _toast = null;
            };
        }

        public Task ShowLoadingAsync(string text)
        {
            if(_progressAlert != null)
            {
                HideLoadingAsync().GetAwaiter().GetResult();
            }

            var result = new TaskCompletionSource<int>();

            var loadingIndicator = new UIActivityIndicatorView(new CGRect(10, 5, 50, 50));
            loadingIndicator.HidesWhenStopped = true;
            loadingIndicator.ActivityIndicatorViewStyle = UIActivityIndicatorViewStyle.Gray;
            loadingIndicator.StartAnimating();

            _progressAlert = UIAlertController.Create(null, text, UIAlertControllerStyle.Alert);
            _progressAlert.View.TintColor = UIColor.Black;
            _progressAlert.View.Add(loadingIndicator);

            var vc = GetPresentedViewController();
            vc?.PresentViewController(_progressAlert, false, () => result.TrySetResult(0));
            return result.Task;
        }

        public Task HideLoadingAsync()
        {
            var result = new TaskCompletionSource<int>();
            if(_progressAlert == null)
            {
                result.TrySetResult(0);
            }
            _progressAlert.DismissViewController(false, () => result.TrySetResult(0));
            _progressAlert.Dispose();
            _progressAlert = null;
            return result.Task;
        }

        private UIViewController GetPresentedViewController()
        {
            var window = UIApplication.SharedApplication.KeyWindow;
            var vc = window.RootViewController;
            while(vc.PresentedViewController != null)
            {
                vc = vc.PresentedViewController;
            }
            return vc;
        }

        private bool TabBarVisible()
        {
            var vc = GetPresentedViewController();
            return vc != null && (vc is UITabBarController ||
                (vc.ChildViewControllers?.Any(c => c is UITabBarController) ?? false));
        }
    }
}