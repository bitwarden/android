using Bit.App.Controls;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedSwitchCell), typeof(ExtendedSwitchCellRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedSwitchCellRenderer : SwitchCellRenderer
    {
        public override UITableViewCell GetCell(Cell item, UITableViewCell reusableCell, UITableView tv)
        {
            var extendedCell = (ExtendedSwitchCell)item;
            var cell = base.GetCell(item, reusableCell, tv);

            if(cell != null)
            {
                cell.BackgroundColor = extendedCell.BackgroundColor.ToUIColor();
            }

            return cell;
        }
    }
}
