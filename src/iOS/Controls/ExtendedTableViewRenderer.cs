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
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);

            var view = (ExtendedTableView)Element;

            CorrectMargins(view);
        }

        private void CorrectMargins(ExtendedTableView view)
        {
            Control.ContentInset = new UIEdgeInsets(-10, 0, -100, 0);
        }
    }
}
