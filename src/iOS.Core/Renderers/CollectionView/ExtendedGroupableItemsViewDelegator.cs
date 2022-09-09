using System;
using Bit.App.Controls;
using Bit.Core.Services;
using CoreGraphics;
using Foundation;
using UIKit;
using Xamarin.Forms.Platform.iOS;

namespace Bit.iOS.Core.Renderers.CollectionView
{
    public class ExtendedGroupableItemsViewDelegator<TItemsView, TViewController> : GroupableItemsViewDelegator<TItemsView, TViewController>
        where TItemsView : ExtendedCollectionView
        where TViewController : GroupableItemsViewController<TItemsView>
    {
        public ExtendedGroupableItemsViewDelegator(ItemsViewLayout itemsViewLayout, TViewController itemsViewController)
            : base(itemsViewLayout, itemsViewController)
        {
        }

        public override CGSize GetSizeForItem(UICollectionView collectionView, UICollectionViewLayout layout, NSIndexPath indexPath)
        {
            // Added this to get extra information on a crash when getting the size for an item.
            try
            {
                return base.GetSizeForItem(collectionView, layout, indexPath);
            }
            catch (Exception ex) when (ViewController?.ItemsView?.ExtraDataForLogging != null)
            {
                var colEx = new CollectionException("Error in ExtendedCollectionView -> ExtendedGroupableItemsViewDelegator, extra data: " + ViewController.ItemsView.ExtraDataForLogging, ex);
                try
                {
                    LoggerHelper.LogEvenIfCantBeResolved(colEx);
                }
                catch
                {
                    // Do nothing in here, this is temporary to get more info about the crash, if the logger fails, we want to get the info
                    // by crashing with the original exception and not the logger one
                }
                throw colEx;
            }
        }
    }
}
