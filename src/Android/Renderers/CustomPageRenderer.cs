using System;
using Android.App;
using Android.Content;
using AndroidX.AppCompat.Widget;
using Bit.App.Resources;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ContentPage), typeof(CustomPageRenderer))]
namespace Bit.Droid.Renderers
{
    public class CustomPageRenderer : PageRenderer
    {
        public CustomPageRenderer(Context context) : base(context)
        {
        }

        protected override void OnElementChanged(ElementChangedEventArgs<Page> e)
        {
            base.OnElementChanged(e);

            Activity context = (Activity)this.Context;
            var toolbar = context.FindViewById<Toolbar>(Resource.Id.toolbar);
            if(toolbar != null)
            {
                toolbar.NavigationContentDescription = AppResources.TapToGoBack;
            }
        }
    }
}
