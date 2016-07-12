using Bit.App.Controls;
using Bit.iOS.Controls;
using System.ComponentModel;
using UIKit;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(SearchBar), typeof(CustomSearchBarRenderer))]
namespace Bit.iOS.Controls
{
    public class CustomSearchBarRenderer : SearchBarRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<SearchBar> e)
        {
            base.OnElementChanged(e);

            var view = e.NewElement;
            if(view != null)
            {
                Control.SearchBarStyle = UISearchBarStyle.Minimal;
                Control.BarStyle = UIBarStyle.BlackTranslucent;
                Control.ShowsCancelButton = Control.IsFirstResponder;
            }
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            var view = Element;
            Control.ShowsCancelButton = Control.IsFirstResponder;
        }
    }
}
