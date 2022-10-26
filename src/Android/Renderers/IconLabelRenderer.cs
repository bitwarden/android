using System;
using Android.Content;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(IconLabel), typeof(IconLabelRenderer))]
namespace Bit.Droid.Renderers
{
    public class IconLabelRenderer : LabelRenderer
    {
        public IconLabelRenderer(Context context) : base(context) { }

        protected override void OnElementChanged(ElementChangedEventArgs<Label> e)
        {
            base.OnElementChanged(e);
            if (e.NewElement != null && e.NewElement is IconLabel iconLabel)
            {
               Control.SetIncludeFontPadding(iconLabel.IncludeFontPadding ?? true);
            }
        }
    }
}
