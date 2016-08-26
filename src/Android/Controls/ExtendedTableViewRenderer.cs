using System;
using Android.Widget;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Android.Content;
using AView = Android.Views.View;
using AListView = Android.Widget.ListView;
using Android.Views;

[assembly: ExportRenderer(typeof(ExtendedTableView), typeof(ExtendedTableViewRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedTableViewRenderer : TableViewRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<TableView> e)
        {
            base.OnElementChanged(e);
            Control.Divider = null;
            Control.DividerHeight = 0;
        }

        protected override TableViewModelRenderer GetModelRenderer(AListView listView, TableView view)
        {
            return new CustomTableViewModelRenderer(Context, listView, view);
        }

        public override SizeRequest GetDesiredSize(int widthConstraint, int heightConstraint)
        {
            var baseSize = base.GetDesiredSize(widthConstraint, heightConstraint);
            var height = ComputeHeight(Control, Convert.ToInt32(baseSize.Request.Width));
            return new SizeRequest(new Size(baseSize.Request.Width, height));
        }

        private int ComputeHeight(AListView listView, int width)
        {
            var element = Element as ExtendedTableView;

            var adapter = listView.Adapter;
            var totalHeight = listView.PaddingTop + listView.PaddingBottom;
            var desiredWidth = MeasureSpec.MakeMeasureSpec(width, MeasureSpecMode.AtMost);
            for(var i = 0; i < adapter.Count; i++)
            {
                if(i == 0 && (element?.NoHeader ?? false))
                {
                    totalHeight += 1;
                    continue;
                }

                var view = adapter.GetView(i, null, listView);
                view.LayoutParameters = new LayoutParams(LayoutParams.WrapContent, LayoutParams.WrapContent);
                view.Measure(desiredWidth, MeasureSpec.MakeMeasureSpec(0, MeasureSpecMode.Unspecified));
                totalHeight += view.MeasuredHeight;
            }

            return totalHeight + (listView.DividerHeight * (adapter.Count - 1));
        }

        private class CustomTableViewModelRenderer : TableViewModelRenderer
        {
            private readonly ExtendedTableView _view;
            private readonly AListView _listView;

            public CustomTableViewModelRenderer(Context context, AListView listView, TableView view)
                : base(context, listView, view)
            {
                _view = view as ExtendedTableView;
                _listView = listView;
            }

            private ITableViewController Controller => _view;

            // ref http://bit.ly/2b9cjnQ
            public override AView GetView(int position, AView convertView, ViewGroup parent)
            {
                var baseView = base.GetView(position, convertView, parent);
                var layout = baseView as LinearLayout;
                if(layout == null)
                {
                    return baseView;
                }

                bool isHeader, nextIsHeader;
                var cell = GetCellForPosition(position, out isHeader, out nextIsHeader);
                if(layout.ChildCount > 0)
                {
                    layout.RemoveViewAt(0);
                    var cellView = CellFactory.GetCell(cell, convertView, parent, Context, _view);
                    layout.AddView(cellView, 0);
                }

                if(isHeader)
                {
                    var textCell = layout.GetChildAt(0) as BaseCellView;
                    if(textCell != null)
                    {
                        if(position == 0 && _view.NoHeader)
                        {
                            textCell.Visibility = ViewStates.Gone;
                        }
                        else
                        {
                            textCell.MainText = textCell.MainText?.ToUpperInvariant();
                            textCell.SetMainTextColor(Color.FromHex("777777"));
                        }
                    }
                }

                var bline = layout.GetChildAt(1);
                if(bline != null)
                {
                    bline.SetBackgroundColor(_view.SeparatorColor.ToAndroid());
                }

                return layout;
            }

            // Copy/pasted from Xamarin source. Invoke via reflection instead maybe?
            private Cell GetCellForPosition(int position, out bool isHeader, out bool nextIsHeader)
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

                        var header = model.GetHeaderCell(sectionIndex);
                        Cell resultCell = null;
                        if(header != null)
                        {
                            resultCell = header;
                        }

                        if(resultCell == null)
                        {
                            resultCell = new TextCell { Text = model.GetSectionTitle(sectionIndex) };
                        }

                        resultCell.Parent = _view;
                        return resultCell;
                    }

                    if(position < size)
                    {
                        nextIsHeader = position == size - 1;
                        return (Cell)model.GetItem(sectionIndex, position - 1);
                    }

                    position -= size;
                }

                return null;
            }
        }
    }
}
