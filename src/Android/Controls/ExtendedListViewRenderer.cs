using System;
using Bit.Android.Controls;
using Bit.App.Controls;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using Android.Content;
using Android.Views;

[assembly: ExportRenderer(typeof(ExtendedListView), typeof(ExtendedListViewRenderer))]
namespace Bit.Android.Controls
{
    public class ExtendedListViewRenderer : ListViewRenderer
    {
        public ExtendedListViewRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<ListView> e)
        {
            base.OnElementChanged(e);

            if(e.NewElement is ExtendedListView listView)
            {
                if(listView.BottomPadding > 0)
                {
                    Control.SetPadding(0, 0, 0, listView.BottomPadding);
                    Control.SetClipToPadding(false);
                    Control.ScrollBarStyle = ScrollbarStyles.OutsideOverlay;
                }
            }
        }
    }
}
