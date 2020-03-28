using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Utilities
{
    public static class iOSHelpers
    {
        public static System.nfloat? GetAccessibleFont<T>(double size)
        {
            var pointSize = UIFontDescriptor.PreferredBody.PointSize;
            if (size == Device.GetNamedSize(NamedSize.Large, typeof(T)))
            {
                pointSize *= 1.3f;
            }
            else if (size == Device.GetNamedSize(NamedSize.Small, typeof(T)))
            {
                pointSize *= .8f;
            }
            else if (size == Device.GetNamedSize(NamedSize.Micro, typeof(T)))
            {
                pointSize *= .6f;
            }
            else if (size != Device.GetNamedSize(NamedSize.Default, typeof(T)))
            {
                // not using dynamic font sizes, return
                return null;
            }
            return pointSize;
        }

        public static void SetBottomBorder(UITextField control)
        {
            control.BorderStyle = UITextBorderStyle.None;
            SetBottomBorder(control as UIView);
        }

        public static void SetBottomBorder(UITextView control)
        {
            SetBottomBorder(control as UIView);
        }

        private static void SetBottomBorder(UIView control)
        {
            var borderLine = new UIView
            {
                BackgroundColor = ((Color)Xamarin.Forms.Application.Current.Resources["BoxBorderColor"]).ToUIColor(),
                TranslatesAutoresizingMaskIntoConstraints = false
            };
            control.AddSubview(borderLine);
            control.AddConstraints(new NSLayoutConstraint[]
            {
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Height, NSLayoutRelation.Equal, 1, 1f),
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Leading, NSLayoutRelation.Equal,
                    control, NSLayoutAttribute.Leading, 1, 0),
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal,
                    control, NSLayoutAttribute.Trailing, 1, 0),
                NSLayoutConstraint.Create(borderLine, NSLayoutAttribute.Top, NSLayoutRelation.Equal,
                    control, NSLayoutAttribute.Bottom, 1, 10f),
            });
        }
    }
}