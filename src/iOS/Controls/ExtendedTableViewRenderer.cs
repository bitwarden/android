using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Controls;
using CoreGraphics;
using Foundation;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedTableView), typeof(ExtendedTableViewRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedTableViewRenderer : TableViewRenderer
    {
        public override SizeRequest GetDesiredSize(double widthConstraint, double heightConstraint)
        {
            Control.LayoutIfNeeded();
            var size = new Size(Control.ContentSize.Width, Control.ContentSize.Height);
            return new SizeRequest(size);
        }

        protected override void OnElementChanged(ElementChangedEventArgs<TableView> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as ExtendedTableView;
            if(view != null)
            {
                SetScrolling(view);
                SetSelection(view);
                UpdateRowHeight(view);
                UpdateEstimatedRowHeight(view);
                UpdateSeparatorColor(view);
                SetSource();
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedTableView)Element;

            if(e.PropertyName == ExtendedTableView.EnableScrollingProperty.PropertyName)
            {
                SetScrolling(view);
            }
            else if(e.PropertyName == ExtendedTableView.RowHeightProperty.PropertyName)
            {
                UpdateRowHeight(view);
            }
            else if(e.PropertyName == ExtendedTableView.EnableSelectionProperty.PropertyName)
            {
                SetSelection(view);
            }
            else if(e.PropertyName == TableView.HasUnevenRowsProperty.PropertyName)
            {
                SetSource();
            }
        }

        private void SetSource()
        {
            var view = (ExtendedTableView)Element;
            if(view.NoFooter || view.NoHeader)
            {
                Control.Source = new CustomTableViewModelRenderer(view);
            }
            else
            {
                Control.Source = Element.HasUnevenRows ? new UnEvenTableViewModelRenderer(Element) : new TableViewModelRenderer(Element);
            }
        }

        private void SetScrolling(ExtendedTableView view)
        {
            Control.ScrollEnabled = view.EnableScrolling;
        }

        private void SetSelection(ExtendedTableView view)
        {
            Control.AllowsSelection = view.EnableSelection;
        }

        private void UpdateRowHeight(ExtendedTableView view)
        {
            var rowHeight = view.RowHeight;
            if(view.HasUnevenRows && rowHeight == -1)
            {
                Control.RowHeight = UITableView.AutomaticDimension;
            }
            else
            {
                Control.RowHeight = rowHeight <= 0 ? 44 : rowHeight;
            }
        }

        private void UpdateEstimatedRowHeight(ExtendedTableView view)
        {
            if(view.HasUnevenRows && view.RowHeight == -1)
            {
                Control.EstimatedRowHeight = view.EstimatedRowHeight;
            }
            else
            {
                Control.EstimatedRowHeight = 0;
            }
        }

        private void UpdateSeparatorColor(ExtendedTableView view)
        {
            Control.SeparatorColor = view.SeparatorColor.ToUIColor(UIColor.Gray);
        }

        public class CustomTableViewModelRenderer : UnEvenTableViewModelRenderer
        {
            private readonly ExtendedTableView _view;

            public CustomTableViewModelRenderer(ExtendedTableView model)
                : base(model)
            {
                _view = model;
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                if(_view.HasUnevenRows)
                {
                    return UITableView.AutomaticDimension;
                }

                return base.GetHeightForRow(tableView, indexPath);
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                if(_view.NoHeader && section == 0)
                {
                    return 0.00001f;
                }

                return base.GetHeightForHeader(tableView, section);
            }

            public override nfloat GetHeightForFooter(UITableView tableView, nint section)
            {
                if(_view.NoFooter && (section + 1) == NumberOfSections(tableView))
                {
                    return 0.00001f;
                }

                return 10f;
            }
        }
    }
}
