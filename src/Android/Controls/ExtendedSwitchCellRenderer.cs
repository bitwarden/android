using Android.Content;
using System.ComponentModel;
using Android.Views;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;
using Android.Widget;

[assembly: ExportRenderer(typeof(ExtendedSwitchCell), typeof(ExtendedSwitchCellRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedSwitchCellRenderer : SwitchCellRenderer
    {
        protected BaseCellView View { get; private set; }

        protected override AView GetCellCore(Cell item, AView convertView, ViewGroup parent, Context context)
        {
            var View = base.GetCellCore(item, convertView, parent, context) as SwitchCellView;
            var extendedCell = (ExtendedSwitchCell)item;

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

                if(item.IsEnabled)
                {
                    View.SetMainTextColor(Color.Black);
                }
                else
                {
                    View.SetMainTextColor(Color.FromHex("777777"));
                }

                if(View.ChildCount > 1)
                {
                    var layout = View.GetChildAt(1) as LinearLayout;
                    if(layout != null && layout.ChildCount > 0)
                    {
                        var textView = layout.GetChildAt(0) as TextView;
                        if(textView != null)
                        {
                            textView.TextSize = (float)Device.GetNamedSize(NamedSize.Medium, typeof(Label));
                        }
                    }
                }
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
