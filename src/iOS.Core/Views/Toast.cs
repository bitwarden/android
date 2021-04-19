using Bit.App.Utilities;
using Foundation;
using System;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Views
{
    public class Toast : UIView
    {
        private NSTimer _dismissTimer;
        private NSLayoutConstraint _heightConstraint;
        private NSLayoutConstraint _leftMarginConstraint;
        private NSLayoutConstraint _rightMarginConstraint;
        private NSLayoutConstraint _bottomMarginConstraint;

        public Toast(string text)
            : base(CoreGraphics.CGRect.FromLTRB(0, 0, 320, 38))
        {
            TranslatesAutoresizingMaskIntoConstraints = false;
            var bgColor = UIColor.DarkGray;
            var nordTheme = ThemeManager.Resources() != null &&
                ThemeManager.GetResourceColor("BackgroundColor") == Color.FromHex("#3b4252");
            if (nordTheme)
            {
                bgColor = Color.FromHex("#4c566a").ToUIColor();
            }
            BackgroundColor = bgColor.ColorWithAlpha(0.9f);
            Layer.CornerRadius = 18;
            Layer.MasksToBounds = true;

            MessageLabel = new UILabel
            {
                TranslatesAutoresizingMaskIntoConstraints = false,
                TextColor = UIColor.White,
                Font = UIFont.SystemFontOfSize(14),
                BackgroundColor = UIColor.Clear,
                LineBreakMode = UILineBreakMode.WordWrap,
                TextAlignment = UITextAlignment.Center,
                Lines = 0,
                Text = text
            };

            AddSubview(MessageLabel);

            var hMessageConstraints = NSLayoutConstraint.FromVisualFormat("H:|-5-[messageLabel]-5-|", 0, new NSDictionary(),
                NSDictionary.FromObjectsAndKeys(new NSObject[] { MessageLabel },
                    new NSObject[] { new NSString("messageLabel") })
            );

            var vMessageConstraints = NSLayoutConstraint.FromVisualFormat("V:|-0-[messageLabel]-0-|", 0, new NSDictionary(),
                NSDictionary.FromObjectsAndKeys(new NSObject[] { MessageLabel },
                    new NSObject[] { new NSString("messageLabel") })
            );

            AddConstraints(hMessageConstraints);
            AddConstraints(vMessageConstraints);

            AddGestureRecognizer(new UITapGestureRecognizer(() => Dismiss(false)));
        }

        public bool Dismissed { get; set; }
        public Action DismissCallback { get; set; }
        public TimeSpan Duration { get; set; } = TimeSpan.FromSeconds(3);
        public UILabel MessageLabel { get; set; }
        public nfloat LeftMargin { get; set; } = 5;
        public nfloat RightMargin { get; set; } = 5;
        public nfloat BottomMargin { get; set; } = 5;
        public nfloat Height { get; set; } = 38;

        public void Show()
        {
            if (Superview != null)
            {
                return;
            }

            _dismissTimer = NSTimer.CreateScheduledTimer(Duration, x => Dismiss());
            LayoutIfNeeded();

            var localSuperView = UIApplication.SharedApplication.KeyWindow;
            if (localSuperView != null)
            {
                localSuperView.AddSubview(this);

                _heightConstraint = NSLayoutConstraint.Create(this, NSLayoutAttribute.Height,
                    NSLayoutRelation.GreaterThanOrEqual, null, NSLayoutAttribute.NoAttribute, 1, Height);

                _leftMarginConstraint = NSLayoutConstraint.Create(this, NSLayoutAttribute.Left, NSLayoutRelation.Equal,
                    localSuperView, NSLayoutAttribute.Left, 1, LeftMargin);

                _rightMarginConstraint = NSLayoutConstraint.Create(this, NSLayoutAttribute.Right, NSLayoutRelation.Equal,
                    localSuperView, NSLayoutAttribute.Right, 1, -RightMargin);

                _bottomMarginConstraint = NSLayoutConstraint.Create(this, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal,
                    localSuperView, NSLayoutAttribute.Bottom, 1, -BottomMargin);

                // Avoid the "UIView-Encapsulated-Layout-Height" constraint conflicts
                // http://stackoverflow.com/questions/25059443/what-is-nslayoutconstraint-uiview-encapsulated-layout-height-and-how-should-i
                _leftMarginConstraint.Priority = 999;
                _rightMarginConstraint.Priority = 999;

                AddConstraint(_heightConstraint);
                localSuperView.AddConstraint(_leftMarginConstraint);
                localSuperView.AddConstraint(_rightMarginConstraint);
                localSuperView.AddConstraint(_bottomMarginConstraint);

                ShowWithAnimation();
            }
            else
            {
                Console.WriteLine("Toast needs a keyWindows to display.");
            }
        }

        public void Dismiss(bool animated = true)
        {
            if (Dismissed)
            {
                return;
            }

            Dismissed = true;
            _dismissTimer?.Invalidate();
            _dismissTimer = null;

            if (!animated)
            {
                RemoveFromSuperview();
                DismissCallback?.Invoke();
                return;
            }

            SetNeedsLayout();
            Animate(0.3f, 0, UIViewAnimationOptions.CurveEaseIn, () => { Alpha = 0; }, () =>
            {
                RemoveFromSuperview();
                DismissCallback?.Invoke();
            });
        }

        private void ShowWithAnimation()
        {
            Alpha = 0;
            SetNeedsLayout();
            _bottomMarginConstraint.Constant = -BottomMargin;
            _leftMarginConstraint.Constant = LeftMargin;
            _rightMarginConstraint.Constant = -RightMargin;
            AnimateNotify(0.3f, 0, 0.7f, 5f, UIViewAnimationOptions.CurveEaseInOut, () => { Alpha = 1; }, null);
        }
    }
}
