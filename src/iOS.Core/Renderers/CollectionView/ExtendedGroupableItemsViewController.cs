using System;
using Bit.App.Controls;
using Bit.Core.Services;
using Foundation;
using UIKit;
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

        protected override UICollectionViewDelegateFlowLayout CreateDelegator()
        {
            return new ExtendedGroupableItemsViewDelegator<TItemsView, ExtendedGroupableItemsViewController<TItemsView>>(ItemsViewLayout, this);
        }

        protected override void UpdateTemplatedCell(TemplatedCell cell, NSIndexPath indexPath)
        {
            try
            {
                base.UpdateTemplatedCell(cell, indexPath);
            }
            catch (Exception ex) when (ItemsView?.ExtraDataForLogging != null)
            {
                var colEx = new CollectionException("Error in ExtendedCollectionView -> ExtendedGroupableItemsViewController, extra data: " + ItemsView.ExtraDataForLogging, ex);
                try
                {
                    LoggerHelper.LogEvenIfCantBeResolved(colEx);
                }
                catch
                {
                    // Do nothing in here, this is temporary to get more info about the crash, if the logger fails, we want to get the info
                    // by crashing with the original exception and not the logger one
                }
                if (ex is IndexOutOfRangeException)
                {
                    return;
                }
                throw colEx;
            }
        }
    }
}
