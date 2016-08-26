using Android.Content;
using System.ComponentModel;
using Android.Views;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;

[assembly: ExportRenderer(typeof(ExtendedViewCell), typeof(ExtendedViewCellRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedViewCellRenderer : ViewCellRenderer
    {
        protected AView View { get; private set; }

        protected override AView GetCellCore(Cell item, AView convertView, ViewGroup parent, Context context)
        {
            var View = base.GetCellCore(item, convertView, parent, context);
            var extendedCell = (ExtendedViewCell)item;

            if(View != null)
            {
                if(extendedCell.BackgroundColor != Color.White)
                {
                    View.SetBackgroundColor(extendedCell.BackgroundColor.ToAndroid());
                }
                else
                {
                    View.SetBackgroundResource(Resource.Drawable.list_selector);
                }
            }

            return View;
        }

        protected override void OnCellPropertyChanged(object sender, PropertyChangedEventArgs args)
        {
            base.OnCellPropertyChanged(sender, args);

            var cell = (ExtendedViewCell)Cell;

            if(args.PropertyName == ExtendedViewCell.BackgroundColorProperty.PropertyName)
            {
                View.SetBackgroundColor(cell.BackgroundColor.ToAndroid());
            }
        }
    }
}
