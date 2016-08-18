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
using Android.Content;
using AView = Android.Views.View;
using AListView = Android.Widget.ListView;
using Android.Views;
using Android.Util;

[assembly: ExportRenderer(typeof(ExtendedTableView), typeof(ExtendedTableViewRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedTableViewRenderer : TableViewRenderer
    {
        protected override TableViewModelRenderer GetModelRenderer(AListView listView, TableView view)
        {
            return new CustomTableViewModelRenderer(Context, listView, view);
        }

        public override SizeRequest GetDesiredSize(int widthConstraint, int heightConstraint)
        {
            var baseSize = base.GetDesiredSize(widthConstraint, heightConstraint);
            var height = ComputeHeight(Control, Convert.ToInt32(baseSize.Request.Width));
            return new SizeRequest(new Xamarin.Forms.Size(baseSize.Request.Width, height));
        }

        private int ComputeHeight(AListView listView, int width)
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

        private class CustomTableViewModelRenderer : TableViewModelRenderer
        {
            private readonly TableView _view;
            private readonly AListView _listView;

            public CustomTableViewModelRenderer(Context context, AListView listView, TableView view)
                : base(context, listView, view)
            {
                _view = view;
                _listView = listView;
            }

            private ITableViewController Controller => _view;

            public override AView GetView(int position, AView convertView, ViewGroup parent)
            {
                var baseView = base.GetView(position, convertView, parent);
                bool isHeader, nextIsHeader;
                GetCellPosition(position, out isHeader, out nextIsHeader);
                if(isHeader)
                {
                    baseView.SetBackgroundColor(Xamarin.Forms.Color.Transparent.ToAndroid());
                }
                else
                {
                    baseView.SetBackgroundColor(Xamarin.Forms.Color.Red.ToAndroid());
                }

                return baseView;
            }

            private void GetCellPosition(int position, out bool isHeader, out bool nextIsHeader)
            {
                isHeader = false;
                nextIsHeader = false;

                var model = Controller.Model;
                var sectionCount = model.GetSectionCount();

                for(var sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++)
                {
                    var size = model.GetRowCount(sectionIndex) + 1;
                    if(position == 0)
                    {
                        isHeader = true;
                        nextIsHeader = size == 0 && sectionIndex < sectionCount - 1;
                    }

                    if(position < size)
                    {
                        nextIsHeader = position == size - 1;
                    }

                    position -= size;
                }
            }
        }
    }
}
