using Android.Content;
using System.ComponentModel;
using Android.Views;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;
using Android.Widget;

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
                View.SetBackgroundColor(extendedCell.BackgroundColor.ToAndroid());


                if(extendedCell.ShowDisclousure)
                {
                    var resourceId = Resource.Drawable.ion_chevron_right;
                    if(!string.IsNullOrWhiteSpace(extendedCell.DisclousureImage))
                    {
                        var fileName = System.IO.Path.GetFileNameWithoutExtension(extendedCell.DisclousureImage);
                        resourceId = context.Resources.GetIdentifier(fileName, "drawable", context.PackageName);
                    }

                    var image = new DisclosureImage(context, extendedCell);
                    image.SetImageResource(resourceId);
                    image.SetPadding(10, 10, 30, 10);
                    //View.SetAccessoryView(image);
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

        private class DisclosureImage : ImageView
        {
            private ExtendedViewCell _cell;

            public DisclosureImage(Context context, ExtendedViewCell cell) : base(context)
            {
                _cell = cell;
            }

            public override bool OnTouchEvent(MotionEvent e)
            {
                switch(e.Action)
                {
                    case MotionEventActions.Up:
                        _cell.OnDisclousureTapped();
                        break;
                    default:
                        break;
                }

                return true;
            }
        }
    }
}
