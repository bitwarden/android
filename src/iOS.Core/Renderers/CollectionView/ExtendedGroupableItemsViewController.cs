using System;
using Bit.App.Controls;
using Foundation;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Renderers.CollectionView
{
    public class ExtendedGroupableItemsViewController<TItemsView> : GroupableItemsViewController<TItemsView>
        where TItemsView : ExtendedCollectionView
    {
        public ExtendedGroupableItemsViewController(TItemsView groupableItemsView, ItemsViewLayout layout)
            : base(groupableItemsView, layout)
        {
        }

        protected override void UpdateTemplatedCell(TemplatedCell cell, NSIndexPath indexPath)
        {
            try
            {
                base.UpdateTemplatedCell(cell, indexPath);
            }
            catch (Exception ex) when (ItemsView?.ExtraDataForLogging != null)
            {
                throw new Exception("Error in ExtendedCollectionView, extra data: " + ItemsView.ExtraDataForLogging, ex);
            }
        }
    }
}
