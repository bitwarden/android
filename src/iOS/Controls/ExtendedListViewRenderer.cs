using Bit.App.Controls;
using System;
using System.ComponentModel;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedListView), typeof(Bit.iOS.Controls.ExtendedListViewRenderer))]
namespace Bit.iOS.Controls
{
    public class ExtendedListViewRenderer : ListViewRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<ListView> e)
        {
            base.OnElementChanged(e);

            // primary color
            Control.SectionIndexColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f);

            if(e.NewElement is ExtendedListView view)
            {
                SetMargin(view);
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if(e.PropertyName == View.MarginProperty.PropertyName)
            {
                SetMargin(Element);
            }
        }

        private void SetMargin(ExtendedListView view)
        {
            Control.ContentInset = new UIEdgeInsets(
                new nfloat(view.Margin.Top),
                new nfloat(view.Margin.Left),
                new nfloat(view.Margin.Bottom),
                new nfloat(view.Margin.Right));
        }
    }
}
