using Bit.App.Controls;
using Bit.iOS.Core.Renderers.CollectionView;
using Xamarin.Forms;
using Xamarin.Forms.Platform.iOS;

[assembly: ExportRenderer(typeof(ExtendedCollectionView), typeof(ExtendedCollectionViewRenderer))]
namespace Bit.iOS.Core.Renderers.CollectionView
{
    public class ExtendedCollectionViewRenderer : GroupableItemsViewRenderer<ExtendedCollectionView, GroupableItemsViewController<ExtendedCollectionView>>
    {
        protected override GroupableItemsViewController<ExtendedCollectionView> CreateController(ExtendedCollectionView itemsView, ItemsViewLayout layout)
        {
            return new ExtendedGroupableItemsViewController<ExtendedCollectionView>(itemsView, layout);
        }
    }
}
