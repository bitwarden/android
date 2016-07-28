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

        public static UIAlertController CreateMessageAlert(string message)
        {
            var alert = UIAlertController.Create(null, message, UIAlertControllerStyle.Alert);
            alert.View.TintColor = UIColor.Black;
            return alert;
        }

        public static UIAlertController CreateAlert(string title, string message, string accept, Action<UIAlertAction> acceptHandle = null)
        {
            var alert = UIAlertController.Create(title, message, UIAlertControllerStyle.Alert);
            var oldFrame = alert.View.Frame;
            alert.View.Frame = new RectangleF((float)oldFrame.X, (float)oldFrame.Y, (float)oldFrame.Width, (float)oldFrame.Height - 20);
            alert.AddAction(UIAlertAction.Create(accept, UIAlertActionStyle.Default, acceptHandle));
            return alert;
        }

        public static UIAlertController CreateActionSheet(string title, UIViewController controller)
        {
            var sheet = UIAlertController.Create(title, null, UIAlertControllerStyle.ActionSheet);
            if(UIDevice.CurrentDevice.UserInterfaceIdiom == UIUserInterfaceIdiom.Pad)
            {
                var x = controller.View.Bounds.Width / 2;
                var y = controller.View.Bounds.Bottom;
                var rect = new CGRect(x, y, 0, 0);

                sheet.PopoverPresentationController.SourceView = controller.View;
                sheet.PopoverPresentationController.SourceRect = rect;
                sheet.PopoverPresentationController.PermittedArrowDirections = UIPopoverArrowDirection.Unknown;
            }
            return sheet;
        }
    }
}
