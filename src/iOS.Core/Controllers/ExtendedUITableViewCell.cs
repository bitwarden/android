using Bit.iOS.Core.Utilities;
using CoreGraphics;
using Foundation;
using ObjCRuntime;
using UIKit;

namespace Bit.iOS.Core.Controllers
{
    public class ExtendedUITableViewCell : UITableViewCell
    {
        public ExtendedUITableViewCell()
        {
            ApplyTheme();
        }

        public ExtendedUITableViewCell(NSCoder coder) : base(coder)
        {
            ApplyTheme();
        }

        public ExtendedUITableViewCell(CGRect frame) : base(frame)
        {
            ApplyTheme();
        }

        public ExtendedUITableViewCell(UITableViewCellStyle style, string reuseIdentifier) : base(style, reuseIdentifier)
        {
            ApplyTheme();
        }

        public ExtendedUITableViewCell(UITableViewCellStyle style, NSString? reuseIdentifier) : base(style, reuseIdentifier)
        {
            ApplyTheme();
        }

        protected ExtendedUITableViewCell(NSObjectFlag t) : base(t)
        {
            ApplyTheme();
        }

        protected internal ExtendedUITableViewCell(NativeHandle handle) : base(handle)
        {
            ApplyTheme();
        }

        private void ApplyTheme()
        {
            BackgroundColor = ThemeHelpers.BackgroundColor;
            if (!ThemeHelpers.LightTheme)
            {
                SelectionStyle = UITableViewCellSelectionStyle.None;
            }
        }
    }
}
