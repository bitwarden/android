using Android.Content;
using Android.Views;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedListView), typeof(ExtendedListViewRenderer))]
namespace Bit.Droid.Renderers
{
    public class ExtendedListViewRenderer : ListViewRenderer
    {
        public ExtendedListViewRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged(ElementChangedEventArgs<ListView> e)
        {
            base.OnElementChanged(e);
            if (Control != null && e.NewElement != null && e.NewElement is ExtendedListView listView)
            {
                // Pad for FAB
                Control.SetPadding(0, 0, 0, 170);
                Control.SetClipToPadding(false);
                Control.ScrollBarStyle = ScrollbarStyles.OutsideOverlay;
            }
        }
    }
}
