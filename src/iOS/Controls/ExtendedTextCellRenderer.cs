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

                        try
                        {
                            detailDisclosureButton.SetImage(UIImage.FromBundle(extendedCell.DisclousureImage + "_selected"), UIControlState.Selected);
                        }
                        catch
                        {
                            detailDisclosureButton.SetImage(UIImage.FromBundle(extendedCell.DisclousureImage), UIControlState.Selected);
                        }

                        detailDisclosureButton.Frame = new CGRect(0f, 0f, 50f, 100f);
                        detailDisclosureButton.TouchUpInside += (sender, e) =>
                        {
                            extendedCell.OnDisclousureTapped();
                        };
                        cell.AccessoryView = detailDisclosureButton;
                    }
                }

                WireUpForceUpdateSizeRequested(item, cell, tv);
                UpdateLineBreakMode(cell.DetailTextLabel, extendedCell.DetailLineBreakMode);
            }

            return cell;
        }

        private void UpdateLineBreakMode(UILabel label, LineBreakMode lineBreakMode)
        {
            if(label == null)
            {
                return;
            }

            switch(lineBreakMode)
            {
                case LineBreakMode.NoWrap:
                    label.LineBreakMode = UILineBreakMode.Clip;
                    label.Lines = 1;
                    break;
                case LineBreakMode.WordWrap:
                    label.LineBreakMode = UILineBreakMode.WordWrap;
                    label.Lines = 0;
                    break;
                case LineBreakMode.CharacterWrap:
                    label.LineBreakMode = UILineBreakMode.CharacterWrap;
                    label.Lines = 0;
                    break;
                case LineBreakMode.HeadTruncation:
                    label.LineBreakMode = UILineBreakMode.HeadTruncation;
                    label.Lines = 1;
                    break;
                case LineBreakMode.MiddleTruncation:
                    label.LineBreakMode = UILineBreakMode.MiddleTruncation;
                    label.Lines = 1;
                    break;
                case LineBreakMode.TailTruncation:
                    label.LineBreakMode = UILineBreakMode.TailTruncation;
                    label.Lines = 1;
                    break;
            }
        }
    }
}
