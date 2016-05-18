using Android.Content;
using System.ComponentModel;
using Android.Views;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;
using Android.Widget;

[assembly: ExportRenderer(typeof(ExtendedTextCell), typeof(ExtendedTextCellRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedTextCellRenderer : TextCellRenderer
    {
        protected AView View { get; private set; }

        protected override AView GetCellCore(Cell item, AView convertView, ViewGroup parent, Context context)
        {
            var View = (BaseCellView)base.GetCellCore(item, convertView, parent, context);
            var extendedCell = (ExtendedTextCell)item;

            if(View != null)
            {
                View.SetBackgroundColor(extendedCell.BackgroundColor.ToAndroid());

                if(extendedCell.ShowDisclousure)
                {
                    // TODO: different image
                    var resourceId = Resource.Drawable.fa_folder_open;
                    if(!string.IsNullOrWhiteSpace(extendedCell.DisclousureImage))
                    {
                        var fileName = System.IO.Path.GetFileNameWithoutExtension(extendedCell.DisclousureImage);
                        resourceId = context.Resources.GetIdentifier(fileName, "drawable", context.PackageName);
                    }

                    var image = new ImageView(context);
                    image.SetImageResource(resourceId);
                    image.SetPadding(0, 0, 30, 0);
                    View.SetAccessoryView(image);
                }
            }

            return View;
        }

        protected override void OnCellPropertyChanged(object sender, PropertyChangedEventArgs args)
        {
            base.OnCellPropertyChanged(sender, args);

            var cell = (ExtendedTextCell)Cell;

            if(args.PropertyName == ExtendedTextCell.BackgroundColorProperty.PropertyName)
            {
                View.SetBackgroundColor(cell.BackgroundColor.ToAndroid());
            }

            // TODO: other properties
        }
    }
}
