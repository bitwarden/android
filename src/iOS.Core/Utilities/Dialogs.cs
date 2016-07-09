using System;
using System.Drawing;
using CoreGraphics;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public static class Dialogs
    {
        public static UIAlertController CreateLoadingAlert(string message)
        {
            var loadingIndicator = new UIActivityIndicatorView(new CGRect(10, 5, 50, 50));
            loadingIndicator.HidesWhenStopped = true;
            loadingIndicator.ActivityIndicatorViewStyle = UIActivityIndicatorViewStyle.Gray;
            loadingIndicator.StartAnimating();

            var alert = UIAlertController.Create(null, message, UIAlertControllerStyle.Alert);
            alert.View.TintColor = UIColor.Black;
            alert.View.Add(loadingIndicator);
            return alert;
        }

        public static UIAlertController CreateAlert(string title, string message, string accept)
        {
            var alert = UIAlertController.Create(title, message, UIAlertControllerStyle.Alert);
            var oldFrame = alert.View.Frame;
            alert.View.Frame = new RectangleF((float)oldFrame.X, (float)oldFrame.Y, (float)oldFrame.Width, (float)oldFrame.Height - 20);
            alert.AddAction(UIAlertAction.Create(accept, UIAlertActionStyle.Default, null));
            return alert;
        }
    }
}
