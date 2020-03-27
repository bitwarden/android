using Bit.iOS.Core.Utilities;
using UIKit;

namespace Bit.iOS.Core.Controllers
{
    public class ExtendedUITableViewCell : UITableViewCell
    {
        public ExtendedUITableViewCell()
        {
            BackgroundColor = ThemeHelpers.BackgroundColor;
            if (!ThemeHelpers.LightTheme)
            {
                SelectionStyle = UITableViewCellSelectionStyle.None;
            }
        }

        public ExtendedUITableViewCell(UITableViewCellStyle style, string reusedId)
            : base(style, reusedId)
        {
            BackgroundColor = ThemeHelpers.BackgroundColor;
            if (!ThemeHelpers.LightTheme)
            {
                SelectionStyle = UITableViewCellSelectionStyle.None;
            }
        }
    }
}
