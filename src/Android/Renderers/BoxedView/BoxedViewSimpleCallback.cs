using Android.Runtime;
using Android.Support.V7.Widget;
using Android.Support.V7.Widget.Helper;
using System;

namespace Bit.Droid.Renderers.BoxedView
{
    [Preserve(AllMembers = true)]
    public class BoxedViewSimpleCallback : ItemTouchHelper.SimpleCallback
    {
        private App.Controls.BoxedView.BoxedView _boxedView;
        private int _offset = 0;

        public BoxedViewSimpleCallback(App.Controls.BoxedView.BoxedView boxedView, int dragDirs, int swipeDirs)
            : base(dragDirs, swipeDirs)
        {
            _boxedView = boxedView;
        }

        public override bool OnMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target)
        {
            if(!(viewHolder is ContentViewHolder fromContentHolder))
            {
                return false;
            }
            if(!(target is ContentViewHolder toContentHolder))
            {
                return false;
            }
            if(fromContentHolder.SectionIndex != toContentHolder.SectionIndex)
            {
                return false;
            }
            var section = _boxedView.Model.GetSection(fromContentHolder.SectionIndex);
            if(section == null || !section.UseDragSort)
            {
                return false;
            }

            var fromPos = viewHolder.AdapterPosition;
            var toPos = target.AdapterPosition;
            _offset += toPos - fromPos;
            var settingsAdapter = recyclerView.GetAdapter() as BoxedViewRecyclerAdapter;
            settingsAdapter.NotifyItemMoved(fromPos, toPos); // rows update
            settingsAdapter.CellMoved(fromPos, toPos); // caches update
            return true;
        }

        public override void ClearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
        {
            base.ClearView(recyclerView, viewHolder);
            if(!(viewHolder is ContentViewHolder contentHolder))
            {
                return;
            }

            var section = _boxedView.Model.GetSection(contentHolder.SectionIndex);
            var pos = contentHolder.RowIndex;
            if(section.ItemsSource == null)
            {
                var tmp = section[pos];
                section.RemoveAt(pos);
                section.Insert(pos + _offset, tmp);
            }
            else if(section.ItemsSource != null)
            {
                // must update DataSource at this timing.
                var tmp = section.ItemsSource[pos];
                section.ItemsSource.RemoveAt(pos);
                section.ItemsSource.Insert(pos + _offset, tmp);
            }
            _offset = 0;
        }

        public override int GetDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
        {
            if(!(viewHolder is ContentViewHolder contentHolder))
            {
                return 0;
            }
            var section = _boxedView.Model.GetSection(contentHolder.SectionIndex);
            if(section == null || !section.UseDragSort)
            {
                return 0;
            }
            return base.GetDragDirs(recyclerView, viewHolder);
        }

        public override void OnSwiped(RecyclerView.ViewHolder viewHolder, int direction)
        {
            throw new NotImplementedException();
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                _boxedView = null;
            }
            base.Dispose(disposing);
        }
    }
}
