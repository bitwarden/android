using System;
using System.ComponentModel;
using Android.Graphics;
using Android.Text;
using Android.Text.Method;
using Android.Views.InputMethods;
using Android.Widget;
using Bit.Android.Controls;
using Bit.App.Controls;
using Bit.App.Enums;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedTableView), typeof(ExtendedTableViewRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedTableViewRenderer : TableViewRenderer
    {
        public override SizeRequest GetDesiredSize(int widthConstraint, int heightConstraint)
        {
            var baseSize = base.GetDesiredSize(widthConstraint, heightConstraint);
            var height = ComputeHeight(Control, Convert.ToInt32(baseSize.Request.Width));
            return new SizeRequest(new Size(baseSize.Request.Width, height));
        }

        private int ComputeHeight(global::Android.Widget.ListView listView, int width)
        {
            var adapter = listView.Adapter;
            var totalHeight = listView.PaddingTop + listView.PaddingBottom;
            var desiredWidth = MeasureSpec.MakeMeasureSpec(width, global::Android.Views.MeasureSpecMode.AtMost);
            for(var i = 0; i < adapter.Count; i++)
            {
                var view = adapter.GetView(i, null, listView);
                view.LayoutParameters = new LayoutParams(LayoutParams.WrapContent, LayoutParams.WrapContent);
                view.Measure(desiredWidth, MeasureSpec.MakeMeasureSpec(0, global::Android.Views.MeasureSpecMode.Unspecified));
                totalHeight += view.MeasuredHeight;
            }

            return totalHeight + (listView.DividerHeight * (adapter.Count - 1));
        }
    }
}
