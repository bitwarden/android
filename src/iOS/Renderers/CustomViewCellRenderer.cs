using Bit.iOS.Renderers;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ViewCell), typeof(CustomViewCellRenderer))]
namespace Bit.iOS.Renderers
{
    public class CustomViewCellRenderer : ViewCellRenderer
    {
        private bool _noSelectionStyle = false;

        public CustomViewCellRenderer()
        {
            _noSelectionStyle = (Color)Xamarin.Forms.Application.Current.Resources["BackgroundColor"] != Color.White;
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
