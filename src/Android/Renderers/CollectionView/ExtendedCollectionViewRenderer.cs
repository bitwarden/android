using System;
using Android.Content;
using AndroidX.RecyclerView.Widget;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Droid.Renderers.CollectionView;
using Bit.Droid.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using static Android.Content.ClipData;
using static AndroidX.RecyclerView.Widget.RecyclerView;

[assembly: ExportRenderer(typeof(ExtendedCollectionView), typeof(ExtendedCollectionViewRenderer))]
namespace Bit.Droid.Renderers.CollectionView
{
    public class CustomGroupableItemsViewAdapter<TItemsView, TItemsViewSource> : GroupableItemsViewAdapter<TItemsView, TItemsViewSource>
        where TItemsView : GroupableItemsView
        where TItemsViewSource : IGroupableItemsViewSource
    {
        protected internal CustomGroupableItemsViewAdapter(TItemsView groupableItemsView, Func<View, Context, ItemContentView> createView = null)
            : base(groupableItemsView, createView)
        {
        }

        public object GetItemAt(int position)
        {
            return ItemsSource.GetItem(position);
        }
    }

    public class ExtendedCollectionViewRenderer : GroupableItemsViewRenderer<ExtendedCollectionView, CustomGroupableItemsViewAdapter<ExtendedCollectionView, IGroupableItemsViewSource>, IGroupableItemsViewSource>
    {
        ItemTouchHelper _itemTouchHelper;

        public ExtendedCollectionViewRenderer(Context context) : base(context)
        {
        }

        protected override CustomGroupableItemsViewAdapter<ExtendedCollectionView, IGroupableItemsViewSource> CreateAdapter()
        {
            return new CustomGroupableItemsViewAdapter<ExtendedCollectionView, IGroupableItemsViewSource>(ItemsView);
        }

        protected override void SetUpNewElement(ExtendedCollectionView newElement)
        {
            base.SetUpNewElement(newElement);

            if (newElement is null)
            {
                return;
            }

            var itemTouchCallback = new RecyclerSwipeItemTouchCallback<CipherViewCellViewModel>(ItemTouchHelper.Right, this.Context, new CipherViewModelSwipeableItem(),
                viewHolder =>
                {
                    if (viewHolder is TemplatedItemViewHolder templatedViewHolder
                        &&
                        templatedViewHolder.View?.BindingContext is CipherViewCellViewModel vm)
                    {
                        return vm;
                    }
                    return null;
                });
            itemTouchCallback.OnSwipedCommand = new Command<ViewHolder>(viewHolder =>
            {
                ItemsViewAdapter.NotifyItemChanged(viewHolder.LayoutPosition);

                ItemsView.OnSwipeCommand?.Execute(ItemsViewAdapter.GetItemAt(viewHolder.BindingAdapterPosition));
            });
            _itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
            _itemTouchHelper.AttachToRecyclerView(this);
        }

        protected override void TearDownOldElement(ItemsView oldElement)
        {
            base.TearDownOldElement(oldElement);

            if (oldElement is null)
            {
                return;
            }

            _itemTouchHelper.AttachToRecyclerView(null);
        }
    }
}
