using Android.Content;
using System.ComponentModel;
using Android.Views;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;
using Android.Widget;
using Android.Text;

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
                if(extendedCell.BackgroundColor != Color.White)
                {
                    View.SetBackgroundColor(extendedCell.BackgroundColor.ToAndroid());
                }
                else
                {
                    View.SetBackgroundResource(Resource.Drawable.list_selector);
                }

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
                    View.SetAccessoryView(image);
                }

                if(View.ChildCount > 1)
                {
                    var layout = View.GetChildAt(1) as LinearLayout;
                    if(layout != null)
                    {
                        if(layout.ChildCount > 0)
                        {
                            var textView = layout.GetChildAt(0) as TextView;
                            if(textView != null)
                            {
                                textView.TextSize = (float)Device.GetNamedSize(NamedSize.Medium, typeof(Label));
                            }
                        }

                        if(layout.ChildCount > 1)
                        {
                            var detailView = layout.GetChildAt(1) as TextView;
                            if(detailView != null)
                            {
                                UpdateLineBreakMode(detailView, extendedCell.DetailLineBreakMode);
                            }
                        }
                    }
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

        private void UpdateLineBreakMode(TextView view, LineBreakMode lineBreakMode)
        {
            if(view == null)
            {
                return;
            }

            switch(lineBreakMode)
            {
                case LineBreakMode.NoWrap:
                    view.SetSingleLine(true);
                    view.Ellipsize = null;
                    break;
                case LineBreakMode.WordWrap:
                    view.SetSingleLine(false);
                    view.Ellipsize = null;
                    view.SetMaxLines(100);
                    break;
                case LineBreakMode.CharacterWrap:
                    view.SetSingleLine(false);
                    view.Ellipsize = null;
                    view.SetMaxLines(100);
                    break;
                case LineBreakMode.HeadTruncation:
                    view.SetSingleLine(true);
                    view.Ellipsize = TextUtils.TruncateAt.Start;
                    break;
                case LineBreakMode.TailTruncation:
                    view.SetSingleLine(true);
                    view.Ellipsize = TextUtils.TruncateAt.End;
                    break;
                case LineBreakMode.MiddleTruncation:
                    view.SetSingleLine(true);
                    view.Ellipsize = TextUtils.TruncateAt.Middle;
                    break;
            }
        }

        private class DisclosureImage : ImageView
        {
            private ExtendedTextCell _cell;

            public DisclosureImage(Context context, ExtendedTextCell cell) : base(context)
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
