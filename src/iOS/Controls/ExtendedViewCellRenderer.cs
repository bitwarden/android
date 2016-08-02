using Bit.App.Controls;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedViewCell), typeof(ExtendedViewCellRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedViewCellRenderer : ViewCellRenderer
    {
        public override UITableViewCell GetCell(Cell item, UITableViewCell reusableCell, UITableView tv)
        {
            var extendedCell = (ExtendedViewCell)item;
            var cell = base.GetCell(item, reusableCell, tv);

            if(cell != null)
            {
                cell.BackgroundColor = extendedCell.BackgroundColor.ToUIColor();
                if(extendedCell.ShowDisclousure)
                {
                    cell.Accessory = UITableViewCellAccessory.DisclosureIndicator;
                }
            }

            WireUpForceUpdateSizeRequested(item, cell, tv);

            return cell;
        }
    }
}
