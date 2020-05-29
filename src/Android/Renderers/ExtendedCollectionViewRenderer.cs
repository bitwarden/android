using Android.Content;
using Android.Views;
using Bit.App.Controls;
using Bit.Droid.Renderers;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(ExtendedCollectionView), typeof(ExtendedCollectionViewRenderer))]
namespace Bit.Droid.Renderers
{
    public class ExtendedCollectionViewRenderer : CollectionViewRenderer
    {
        public ExtendedCollectionViewRenderer(Context context)
            : base(context)
        { }

        protected override void OnElementChanged (ElementChangedEventArgs<ItemsView> e)
        {
            base.OnElementChanged(e);

            // This may not be necessary... we'll see.
            if (View != null && e.NewElement != null && e.NewElement is ExtendedCollectionView listView)
            {
                // Pad for FAB
                View.SetPadding(0, 0, 0, 170);
                //View.SetClipToPadding(false);
                View.ScrollBarStyle = ScrollbarStyles.OutsideOverlay;
            }
        }
    }
}
