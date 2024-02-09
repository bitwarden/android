using Bit.App.Utilities;
using UIKit;

namespace Bit.iOS.Core.Handlers
{
    //This is a Compatibility version of the ViewCellRenderer. Eventually we should see if there's a better way to implement this behavior.
    public class CustomViewCellHandler : Microsoft.Maui.Controls.Handlers.Compatibility.ViewCellRenderer
    {
        private bool _noSelectionStyle = false;

        public CustomViewCellHandler()
        {
            _noSelectionStyle = !(ThemeManager.GetResourceColor("BackgroundColor").Equals(Colors.White));
        }

        public override UITableViewCell GetCell(Cell item, UITableViewCell reusableCell, UITableView tv)
        {
            var cell = base.GetCell(item, reusableCell, tv);
            if (_noSelectionStyle)
            {
                cell.SelectionStyle = UITableViewCellSelectionStyle.None;
            }
            return cell;
        }
    }
}
