using Bit.App.Controls;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;
using CoreGraphics;

[assembly: ExportRenderer(typeof(ExtendedTextCell), typeof(ExtendedTextCellRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedTextCellRenderer : TextCellRenderer
    {
        public override UITableViewCell GetCell(Cell item, UITableViewCell reusableCell, UITableView tv)
        {
            var extendedCell = (ExtendedTextCell)item;
            var cell = base.GetCell(item, reusableCell, tv);

            if(cell != null)
            {
                cell.BackgroundColor = extendedCell.BackgroundColor.ToUIColor();
                if(extendedCell.ShowDisclousure)
                {
                    cell.Accessory = UITableViewCellAccessory.DisclosureIndicator;
                    if(!string.IsNullOrEmpty(extendedCell.DisclousureImage))
                    {
                        var detailDisclosureButton = UIButton.FromType(UIButtonType.Custom);
                        detailDisclosureButton.SetImage(UIImage.FromBundle(extendedCell.DisclousureImage), UIControlState.Normal);
                        detailDisclosureButton.SetImage(UIImage.FromBundle(extendedCell.DisclousureImage), UIControlState.Selected);

                        detailDisclosureButton.Frame = new CGRect(0f, 0f, 30f, 30f);
                        detailDisclosureButton.TouchUpInside += (sender, e) =>
                        {
                            var index = tv.IndexPathForCell(cell);
                            tv.SelectRow(index, true, UITableViewScrollPosition.None);
                            tv.Source.AccessoryButtonTapped(tv, index);
                        };
                        cell.AccessoryView = detailDisclosureButton;
                    }
                }
            }

            return cell;
        }
    }
}
