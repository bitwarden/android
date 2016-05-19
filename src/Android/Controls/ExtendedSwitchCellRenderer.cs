using Android.Content;
using System.ComponentModel;
using Android.Views;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;

[assembly: ExportRenderer(typeof(ExtendedSwitchCell), typeof(ExtendedSwitchCellRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedSwitchCellRenderer : SwitchCellRenderer
    {
        protected AView View { get; private set; }

        protected override AView GetCellCore(Cell item, AView convertView, ViewGroup parent, Context context)
        {
            var View = base.GetCellCore(item, convertView, parent, context);
            var extendedCell = (ExtendedSwitchCell)item;

            if(View != null)
            {
                View.SetBackgroundColor(extendedCell.BackgroundColor.ToAndroid());
            }

            return View;
        }

        protected override void OnCellPropertyChanged(object sender, PropertyChangedEventArgs args)
        {
            base.OnCellPropertyChanged(sender, args);

            var cell = (ExtendedSwitchCell)Cell;

            if(args.PropertyName == ExtendedSwitchCell.BackgroundColorProperty.PropertyName)
            {
                View.SetBackgroundColor(cell.BackgroundColor.ToAndroid());
            }
        }
    }
}
