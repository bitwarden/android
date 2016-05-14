using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.iOS.Controls;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedTableView), typeof(ExtendedTableViewRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedTableViewRenderer : TableViewRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<TableView> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as ExtendedTableView;
            if(view != null)
            {
                CorrectMargins(view);
                SetScrolling(view);
                SetSelection(view);
                UpdateRowHeight(view);
                UpdateEstimatedRowHeight(view);
                UpdateSeparatorColor(view);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedTableView)Element;

            CorrectMargins(view);

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
        }

        private void CorrectMargins(ExtendedTableView view)
        {
            Control.ContentInset = new UIEdgeInsets(-10, 0, -100, 0);
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
    }
}
