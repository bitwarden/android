using System;
using System.ComponentModel;
using Bit.App.Controls;
using Bit.UWP.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.UWP;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;


[assembly: ExportRenderer(typeof(ExtendedTableView), typeof(ExtendedTableViewRenderer))]
namespace Bit.UWP.Controls
{
    public class ExtendedTableViewRenderer : TableViewRenderer
    {
        public override SizeRequest GetDesiredSize(double widthConstraint, double heightConstraint)
        {
            var baseSize = new Size(Control.Width, Control.Height);

            return new SizeRequest(new Size(baseSize.Width, baseSize.Height));
        }

        protected override void OnElementChanged(ElementChangedEventArgs<TableView> e)
        {
            base.OnElementChanged(e);
        }
    }
}
