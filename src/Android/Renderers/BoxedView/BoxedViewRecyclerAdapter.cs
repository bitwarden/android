using Android.Content;
using Android.Runtime;
using Android.Support.V7.Widget;
using Android.Text;
using Android.Views;
using Android.Widget;
using Bit.App.Controls.BoxedView;
using System;
using System.Collections.Generic;
using System.Linq;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using AView = Android.Views.View;

namespace Bit.Droid.Renderers.BoxedView
{
    [Preserve(AllMembers = true)]
    public class BoxedViewRecyclerAdapter : RecyclerView.Adapter, AView.IOnClickListener
    {
        private const int ViewTypeHeader = 0;
        private const int ViewTypeFooter = 1;

        private Dictionary<Type, int> _viewTypes;
        private List<CellCache> _cellCaches;

        internal List<CellCache> CellCaches
        {
            get
            {
                if(_cellCaches == null)
                {
                    FillCache();
                }
                return _cellCaches;
            }
        }

        // Item click. correspond to AdapterView.IOnItemClickListener
        private int _selectedIndex = -1;
        private AView _preSelectedCell = null;

        Context _context;
        App.Controls.BoxedView.BoxedView _boxedView;
        RecyclerView _recyclerView;

        List<ViewHolder> _viewHolders = new List<ViewHolder>();

        public BoxedViewRecyclerAdapter(Context context, App.Controls.BoxedView.BoxedView boxedView,
            RecyclerView recyclerView)
        {
            _context = context;
            _boxedView = boxedView;
            _recyclerView = recyclerView;
            _boxedView.ModelChanged += BoxedView_ModelChanged;
        }

        private float MinRowHeight => _context.ToPixels(44);

        public override int ItemCount => CellCaches.Count;

        public override long GetItemId(int position)
        {
            return position;
        }

        public override RecyclerView.ViewHolder OnCreateViewHolder(ViewGroup parent, int viewType)
        {
            ViewHolder viewHolder;
            switch(viewType)
            {
                case ViewTypeHeader:
                    viewHolder = new HeaderViewHolder(
                        LayoutInflater.FromContext(_context).Inflate(Resource.Layout.HeaderCell, parent, false),
                        _boxedView);
                    break;
                case ViewTypeFooter:
                    viewHolder = new FooterViewHolder(
                        LayoutInflater.FromContext(_context).Inflate(Resource.Layout.FooterCell, parent, false),
                        _boxedView);
                    break;
                default:
                    viewHolder = new ContentViewHolder(
                        LayoutInflater.FromContext(_context).Inflate(Resource.Layout.ContentCell, parent, false));
                    viewHolder.ItemView.SetOnClickListener(this);
                    break;
            }
            _viewHolders.Add(viewHolder);
            return viewHolder;
        }

        public void OnClick(AView view)
        {
            var position = _recyclerView.GetChildAdapterPosition(view);

            // TODO: It is desirable that the forms side has Selected property and reflects it.
            // But do it at a later as iOS side doesn't have that process.
            DeselectRow();

            var cell = view.FindViewById<LinearLayout>(Resource.Id.ContentCellBody).GetChildAt(0) as BaseCellView;
            if(cell == null || !CellCaches[position].Cell.IsEnabled)
            {
                // If FormsCell IsEnable is false, does nothing. 
                return;
            }

            _boxedView.Model.RowSelected(CellCaches[position].Cell);
            cell.RowSelected(this, position);
        }

        public override void OnBindViewHolder(RecyclerView.ViewHolder holder, int position)
        {
            var cellInfo = CellCaches[position];
            switch(holder.ItemViewType)
            {
                case ViewTypeHeader:
                    BindHeaderView((HeaderViewHolder)holder, (TextCell)cellInfo.Cell);
                    break;
                case ViewTypeFooter:
                    BindFooterView((FooterViewHolder)holder, (TextCell)cellInfo.Cell);
                    break;
                default:
                    BindContentView((ContentViewHolder)holder, cellInfo.Cell, position);
                    break;
            }
        }

        public override int GetItemViewType(int position)
        {
            var cellInfo = CellCaches[position];
            if(cellInfo.IsHeader)
            {
                return ViewTypeHeader;
            }
            else if(cellInfo.IsFooter)
            {
                return ViewTypeFooter;
            }
            else
            {
                return _viewTypes[cellInfo.Cell.GetType()];
            }
        }

        public void DeselectRow()
        {
            if(_preSelectedCell != null)
            {
                _preSelectedCell.Selected = false;
                _preSelectedCell = null;
            }
            _selectedIndex = -1;
        }

        public void SelectedRow(AView cell, int position)
        {
            _preSelectedCell = cell;
            _selectedIndex = position;
            cell.Selected = true;
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                _boxedView.ModelChanged -= BoxedView_ModelChanged;
                _cellCaches?.Clear();
                _cellCaches = null;
                _boxedView = null;
                _viewTypes = null;
                foreach(var holder in _viewHolders)
                {
                    holder.Dispose();
                }
                _viewHolders.Clear();
                _viewHolders = null;
            }
            base.Dispose(disposing);
        }

        private void BoxedView_ModelChanged(object sender, EventArgs e)
        {
            if(_recyclerView != null)
            {
                _cellCaches = null;
                NotifyDataSetChanged();
            }
        }

        private void BindHeaderView(HeaderViewHolder holder, TextCell formsCell)
        {
            var view = holder.ItemView;

            // Judging cell height
            int cellHeight = (int)_context.ToPixels(44);
            var individualHeight = formsCell.Height;

            if(individualHeight > 0d)
            {
                cellHeight = (int)_context.ToPixels(individualHeight);
            }
            else if(_boxedView.HeaderHeight > -1)
            {
                cellHeight = (int)_context.ToPixels(_boxedView.HeaderHeight);
            }

            view.SetMinimumHeight(cellHeight);
            view.LayoutParameters.Height = cellHeight;

            holder.TextView.SetPadding(
                (int)view.Context.ToPixels(_boxedView.HeaderPadding.Left),
                (int)view.Context.ToPixels(_boxedView.HeaderPadding.Top),
                (int)view.Context.ToPixels(_boxedView.HeaderPadding.Right),
                (int)view.Context.ToPixels(_boxedView.HeaderPadding.Bottom));

            holder.TextView.Gravity = _boxedView.HeaderTextVerticalAlign.ToAndroidVertical() | GravityFlags.Left;
            holder.TextView.TextAlignment = Android.Views.TextAlignment.Gravity;
            holder.TextView.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)_boxedView.HeaderFontSize);
            holder.TextView.SetBackgroundColor(_boxedView.HeaderBackgroundColor.ToAndroid());
            holder.TextView.SetMaxLines(1);
            holder.TextView.SetMinLines(1);
            holder.TextView.SetTypeface(null, Android.Graphics.TypefaceStyle.Bold);
            holder.TextView.Ellipsize = TextUtils.TruncateAt.End;

            if(_boxedView.HeaderTextColor != Color.Default)
            {
                holder.TextView.SetTextColor(_boxedView.HeaderTextColor.ToAndroid());
            }

            // Border setting
            if(_boxedView.ShowSectionTopBottomBorder)
            {
                holder.Border.SetBackgroundColor(_boxedView.SeparatorColor.ToAndroid());
            }
            else
            {
                holder.Border.SetBackgroundColor(Android.Graphics.Color.Transparent);
            }

            // Update text
            holder.TextView.Text = formsCell.Text;
        }

        private void BindFooterView(FooterViewHolder holder, TextCell formsCell)
        {
            var view = holder.ItemView;

            // Footer visible setting
            if(string.IsNullOrEmpty(formsCell.Text))
            {
                //if text is empty, hidden (height 0)
                holder.TextView.Visibility = ViewStates.Gone;
                view.Visibility = ViewStates.Gone;
            }
            else
            {
                holder.TextView.Visibility = ViewStates.Visible;
                view.Visibility = ViewStates.Visible;
            }

            holder.TextView.SetPadding(
                (int)view.Context.ToPixels(_boxedView.FooterPadding.Left),
                (int)view.Context.ToPixels(_boxedView.FooterPadding.Top),
                (int)view.Context.ToPixels(_boxedView.FooterPadding.Right),
                (int)view.Context.ToPixels(_boxedView.FooterPadding.Bottom));

            holder.TextView.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)_boxedView.FooterFontSize);
            holder.TextView.SetBackgroundColor(_boxedView.FooterBackgroundColor.ToAndroid());
            if(_boxedView.FooterTextColor != Color.Default)
            {
                holder.TextView.SetTextColor(_boxedView.FooterTextColor.ToAndroid());
            }

            // Update text
            holder.TextView.Text = formsCell.Text;
        }

        private void BindContentView(ContentViewHolder holder, Cell formsCell, int position)
        {
            AView nativeCell = null;
            AView layout = holder.ItemView;

            holder.SectionIndex = CellCaches[position].SectionIndex;
            holder.RowIndex = CellCaches[position].RowIndex;

            nativeCell = holder.Body.GetChildAt(0);
            if(nativeCell != null)
            {
                holder.Body.RemoveViewAt(0);
            }

            nativeCell = CellFactory.GetCell(formsCell, nativeCell, _recyclerView, _context, _boxedView);

            if(position == _selectedIndex)
            {
                DeselectRow();
                nativeCell.Selected = true;
                _preSelectedCell = nativeCell;
            }

            var minHeight = (int)Math.Max(_context.ToPixels(_boxedView.RowHeight), MinRowHeight);

            // It is necessary to set both
            layout.SetMinimumHeight(minHeight);
            nativeCell.SetMinimumHeight(minHeight);

            if(!_boxedView.HasUnevenRows)
            {
                // If not Uneven, set the larger one of RowHeight and MinRowHeight.
                layout.LayoutParameters.Height = minHeight;
            }
            else if(formsCell.Height > -1)
            {
                // If the cell itself was specified height, set it.
                layout.SetMinimumHeight((int)_context.ToPixels(formsCell.Height));
                layout.LayoutParameters.Height = (int)_context.ToPixels(formsCell.Height);
            }
            else if(formsCell is ViewCell viewCell)
            {
                // If used a viewcell, calculate the size and layout it.
                var size = viewCell.View.Measure(_boxedView.Width, double.PositiveInfinity);
                viewCell.View.Layout(new Rectangle(0, 0, size.Request.Width, size.Request.Height));
                layout.LayoutParameters.Height = (int)_context.ToPixels(size.Request.Height);
            }
            else
            {
                layout.LayoutParameters.Height = -2; // wrap_content
            }

            if(!CellCaches[position].IsLastCell || _boxedView.ShowSectionTopBottomBorder)
            {
                holder.Border.SetBackgroundColor(_boxedView.SeparatorColor.ToAndroid());
            }
            else
            {
                holder.Border.SetBackgroundColor(Android.Graphics.Color.Transparent);
            }

            holder.Body.AddView(nativeCell, 0);
        }

        private void FillCache()
        {
            var model = _boxedView.Model;
            int sectionCount = model.GetSectionCount();

            var newCellCaches = new List<CellCache>();
            for(var sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++)
            {
                var sectionTitle = model.GetSectionTitle(sectionIndex);
                var sectionRowCount = model.GetRowCount(sectionIndex);

                Cell headerCell = new TextCell { Text = sectionTitle, Height = model.GetHeaderHeight(sectionIndex) };
                headerCell.Parent = _boxedView;

                newCellCaches.Add(new CellCache
                {
                    Cell = headerCell,
                    IsHeader = true,
                    SectionIndex = sectionIndex,
                });

                for(int i = 0; i < sectionRowCount; i++)
                {
                    newCellCaches.Add(new CellCache
                    {
                        Cell = model.GetCell(sectionIndex, i),
                        IsLastCell = i == sectionRowCount - 1,
                        SectionIndex = sectionIndex,
                        RowIndex = i
                    });
                }

                var footerCell = new TextCell { Text = model.GetFooterText(sectionIndex) };
                footerCell.Parent = _boxedView;

                newCellCaches.Add(new CellCache
                {
                    Cell = footerCell,
                    IsFooter = true,
                    SectionIndex = sectionIndex,
                });
            }

            _cellCaches = newCellCaches;

            if(_viewTypes == null)
            {
                _viewTypes = _cellCaches
                    .Select(x => x.Cell.GetType())
                    .Distinct()
                    .Select((x, idx) => new { x, index = idx })
                    .ToDictionary(key => key.x, val => val.index + 2);
            }
            else
            {
                var idx = _viewTypes.Values.Max() + 1;
                foreach(var t in _cellCaches.Select(x => x.Cell.GetType()).Distinct().Except(_viewTypes.Keys).ToList())
                {
                    _viewTypes.Add(t, idx++);
                }
            }
        }

        public void CellMoved(int fromPos, int toPos)
        {
            var tmp = CellCaches[fromPos];
            CellCaches.RemoveAt(fromPos);
            CellCaches.Insert(toPos, tmp);
        }

        [Preserve(AllMembers = true)]
        internal class CellCache
        {
            public Cell Cell { get; set; }
            public bool IsHeader { get; set; } = false;
            public bool IsFooter { get; set; } = false;
            public bool IsLastCell { get; set; } = false;
            public int SectionIndex { get; set; }
            public int RowIndex { get; set; }
        }
    }

    [Preserve(AllMembers = true)]
    internal class ViewHolder : RecyclerView.ViewHolder
    {
        public ViewHolder(AView view)
            : base(view) { }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                ItemView?.Dispose();
                ItemView = null;
            }
            base.Dispose(disposing);
        }
    }

    [Preserve(AllMembers = true)]
    internal class HeaderViewHolder : ViewHolder
    {
        public HeaderViewHolder(AView view, App.Controls.BoxedView.BoxedView boxedView)
            : base(view)
        {
            TextView = view.FindViewById<TextView>(Resource.Id.HeaderCellText);
            Border = view.FindViewById<LinearLayout>(Resource.Id.HeaderCellBorder);
        }

        public TextView TextView { get; private set; }
        public LinearLayout Border { get; private set; }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                TextView?.Dispose();
                TextView = null;
                Border?.Dispose();
                Border = null;
            }
            base.Dispose(disposing);
        }
    }

    [Preserve(AllMembers = true)]
    internal class FooterViewHolder : ViewHolder
    {
        public TextView TextView { get; private set; }

        public FooterViewHolder(AView view, App.Controls.BoxedView.BoxedView boxedView)
            : base(view)
        {
            TextView = view.FindViewById<TextView>(Resource.Id.FooterCellText);
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                TextView?.Dispose();
                TextView = null;
            }
            base.Dispose(disposing);
        }
    }

    [Preserve(AllMembers = true)]
    internal class ContentViewHolder : ViewHolder
    {
        public LinearLayout Body { get; private set; }
        public AView Border { get; private set; }
        public int SectionIndex { get; set; }
        public int RowIndex { get; set; }

        public ContentViewHolder(AView view)
            : base(view)
        {
            Body = view.FindViewById<LinearLayout>(Resource.Id.ContentCellBody);
            Border = view.FindViewById(Resource.Id.ContentCellBorder);
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                var nativeCell = Body.GetChildAt(0);
                if(nativeCell is INativeElementView nativeElementView)
                {
                    // If a ViewCell is used, it stops the ViewCellContainer from executing the dispose method.
                    // Because if the AiForms.Effects is used and a ViewCellContainer is disposed, it crashes.
                    if(!(nativeElementView.Element is ViewCell))
                    {
                        nativeCell?.Dispose();
                    }
                }
                Border?.Dispose();
                Border = null;
                Body?.Dispose();
                Body = null;
                ItemView.SetOnClickListener(null);
            }
            base.Dispose(disposing);
        }
    }
}
