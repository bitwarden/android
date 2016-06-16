using System;
using System.ComponentModel;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ListView), typeof(Bit.iOS.Controls.ListViewRenderer))]
namespace Bit.iOS.Controls
{
    public class ListViewRenderer : Xamarin.Forms.Platform.iOS.ListViewRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<ListView> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement as ListView;
            if(view != null)
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

        private void SetMargin(ListView view)
        {
            Control.ContentInset = new UIEdgeInsets(
                new nfloat(view.Margin.Top),
                new nfloat(view.Margin.Left),
                new nfloat(view.Margin.Bottom),
                new nfloat(view.Margin.Right));
        }
    }
}
