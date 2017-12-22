using Foundation;
using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class Toast : UIView
    {
        // Timer to dismiss the snack bar.
        private NSTimer _dismissTimer;

        // Constraints.
        private NSLayoutConstraint _heightConstraint;
        private NSLayoutConstraint _leftMarginConstraint;
        private NSLayoutConstraint _rightMarginConstraint;
        private NSLayoutConstraint _topMarginConstraint;
        private NSLayoutConstraint _bottomMarginConstraint;

        public Toast(string text) : base(CoreGraphics.CGRect.FromLTRB(0, 0, 320, 44))
        {
            TranslatesAutoresizingMaskIntoConstraints = false;
            BackgroundColor = UIColor.DarkGray;
            Layer.CornerRadius = 4;
            Layer.MasksToBounds = true;

            MessageLabel = new UILabel
            {
                TranslatesAutoresizingMaskIntoConstraints = false,
                TextColor = UIColor.White,
                Font = UIFont.SystemFontOfSize(14),
                BackgroundColor = UIColor.Clear,
                LineBreakMode = UILineBreakMode.WordWrap,
                TextAlignment = UITextAlignment.Center,
                Lines = 0
            };

            var hConstraints = NSLayoutConstraint.FromVisualFormat("H:|-5-[messageLabel]-5-|", 0, new NSDictionary(),
                NSDictionary.FromObjectsAndKeys(new NSObject[] { MessageLabel },
                    new NSObject[] { new NSString("messageLabel") })
            );

            var vMessageConstraints = NSLayoutConstraint.FromVisualFormat("V:|-0-[messageLabel]-0-|", 0, new NSDictionary(),
                NSDictionary.FromObjectsAndKeys(new NSObject[] { MessageLabel },
                    new NSObject[] { new NSString("messageLabel") })
            );

            AddConstraints(hConstraints);
            AddConstraints(vMessageConstraints);
            AddSubview(MessageLabel);
        }

        public TimeSpan Duration { get; set; } = TimeSpan.FromSeconds(3);
        public UILabel MessageLabel { get; set; }
        public nfloat LeftMargin { get; set; } = 4;
        public nfloat RightMargin { get; set; } = 4;
        public nfloat BottomMargin { get; set; } = 4;
        public nfloat Height { get; set; } = 44;
        public nfloat TopMargin { get; set; } = 8;

        public void Show()
        {
            if(Superview != null)
            {
                return;
            }

            _dismissTimer = NSTimer.CreateScheduledTimer(Duration, x => Dismiss());
            LayoutIfNeeded();

            var localSuperView = UIApplication.SharedApplication.KeyWindow;
            if(localSuperView != null)
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

                // Show
                ShowWithAnimation();
            }
            else
            {
                Console.WriteLine("Toast needs a keyWindows to display.");
            }
        }

        public void Dismiss(bool animated = true)
        {
            _dismissTimer?.Invalidate();
            _dismissTimer = null;
            nfloat superViewWidth = 0;

            if(Superview != null)
            {
                superViewWidth = Superview.Frame.Width;
            }

            if(!animated)
            {
                RemoveFromSuperview();
                return;
            }

            SetNeedsLayout();
            Animate(0.3f, 0, UIViewAnimationOptions.CurveEaseIn, () => { Alpha = 0; }, RemoveFromSuperview);
        }

        private void ShowWithAnimation()
        {
            _bottomMarginConstraint.Constant = -BottomMargin;
            _leftMarginConstraint.Constant = LeftMargin;
            _rightMarginConstraint.Constant = -RightMargin;
            AnimateNotify(0.3f, 0, 0.7f, 5f, UIViewAnimationOptions.CurveEaseInOut, () => { Alpha = 0; }, null);
        }
    }
}
