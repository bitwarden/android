using System;
using Android.App;
using Android.Content;
using AndroidX.AppCompat.Widget;
using Bit.App.Resources;
using Bit.App.Droid.Renderers;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Microsoft.Maui.Controls.Platform;
using Toolbar = AndroidX.AppCompat.Widget.Toolbar;

namespace Bit.App.Droid.Renderers
{
    public class CustomPageRenderer : PageRenderer
    {
        public CustomPageRenderer(Context context) : base(context)
        {
        }

        protected override void OnElementChanged(ElementChangedEventArgs<Page> e)
        {
            base.OnElementChanged(e);

            // TODO: [MAUI-Migration] [Critical]
            //Activity context = (Activity)this.Context;
            //var toolbar = context.FindViewById<Toolbar>(Android.Resource.Id.toolbar);
            //if(toolbar != null)
            //{
            //    toolbar.NavigationContentDescription = AppResources.TapToGoBack;
            //}
        }
    }
}
