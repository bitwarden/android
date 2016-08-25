using System;
using System.ComponentModel;
using Bit.Android.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(SearchBar), typeof(CustomSearchBarRenderer))]
namespace Bit.Android.Controls
{
    public class CustomSearchBarRenderer : SearchBarRenderer
    {
        protected override void OnElementChanged(ElementChangedEventArgs<SearchBar> e)
        {
            base.OnElementChanged(e);
            Control.SetPadding((int)global::Android.App.Application.Context.ToPixels(-8), 0, 0, 0);
        }
    }
}
