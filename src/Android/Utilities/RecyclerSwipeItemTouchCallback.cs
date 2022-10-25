using System;
using System.Collections.Generic;
using System.Windows.Input;
using Android.Graphics;
using Android.Graphics.Drawables;
using Android.Util;
using Android.Views;
using AndroidX.RecyclerView.Widget;
using Bit.App.Abstractions;
using Xamarin.Forms.Platform.Android;
using FontImageSource = Xamarin.Forms.FontImageSource;

namespace Bit.Droid.Utilities
{
    public class RecyclerSwipeItemTouchCallback<TItem> : ItemTouchHelper.SimpleCallback
    {
        private Paint _clearPaint;
        private readonly ColorDrawable _background = new ColorDrawable();
        private readonly Android.Content.Context _context;
        private readonly ISwipeableItem<TItem> _swipeableItem;
        private readonly Func<RecyclerView.ViewHolder, TItem> _viewHolderToTItem;
        private Dictionary<string, Typeface> _fontFamilyTypefaceCache = new Dictionary<string, Typeface>();

        public RecyclerSwipeItemTouchCallback(int swipeDir, Android.Content.Context context, ISwipeableItem<TItem> swipeableItem, Func<RecyclerView.ViewHolder, TItem> viewHolderToTItem)
            : base(0, swipeDir)
        {
            _context = context;
            _swipeableItem = swipeableItem;
            _viewHolderToTItem = viewHolderToTItem;

            _clearPaint = new Paint();
            _clearPaint.SetXfermode(new PorterDuffXfermode(PorterDuff.Mode.Clear));
        }

        public ICommand OnSwipedCommand { get; set; }

        public override bool OnMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
        {
            return false;
        }

        public override void OnChildDrawOver(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, bool isCurrentlyActive)
        {

            var itemView = viewHolder.ItemView;
            int itemHeight = itemView.Bottom - itemView.Top;
            var isCanceled = (dX == 0f) && !isCurrentlyActive;

            if (isCanceled)
            {
                ClearCanvas(c, itemView.Right + dX, itemView.Top, itemView.Right, itemView.Bottom);
                base.OnChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
                return;
            }

            if (!(_swipeableItem.GetSwipeIcon(_viewHolderToTItem(viewHolder)) is FontImageSource fontSource))
            {
                return;
            }

            using var paint = GetIconPaint(itemView, fontSource);

            var width = (int)(paint.MeasureText(fontSource.Glyph) + .5f);
            var baseline = (int)(-paint.Ascent() + .5f);
            var height = (int)(baseline + paint.Descent() + .5f);

            int itemTop = itemView.Top + (itemHeight - height) / 2;
            int itemMargin = (itemHeight - height) / 2;
            int itemBottom = itemTop + height;

            _background.Color = _swipeableItem.GetBackgroundColor(_viewHolderToTItem(viewHolder)).ToAndroid();
            if (dX < 0)
            {
                _background.SetBounds((int)(itemView.Right + dX), itemView.Top, itemView.Right, itemView.Bottom);
                _background.Draw(c);

                int itemLeft = itemView.Right - itemMargin - width;
                int itemRight = itemView.Right - itemMargin;

                c.DrawText(fontSource.Glyph, itemLeft, itemBottom, paint);
            }
            else
            {
                _background.SetBounds((int)(itemView.Left + dX), itemView.Top, itemView.Left, itemView.Bottom);
                _background.Draw(c);

                int itemLeft = itemView.Left + itemMargin;
                int itemRight = itemView.Left + itemMargin + width;

                c.DrawText(fontSource.Glyph, itemLeft, itemBottom, paint);
            }

            base.OnChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        private Paint GetIconPaint(View itemView, FontImageSource fontSource)
        {
            var paint = new Paint
            {
                TextSize = TypedValue.ApplyDimension(ComplexUnitType.Dip, (float)fontSource.Size, _context.Resources.DisplayMetrics),
                Color = fontSource.Color.ToAndroid(),
                TextAlign = Paint.Align.Left,
                AntiAlias = true,
            };

            if (fontSource.FontFamily != null)
            {
                if (!_fontFamilyTypefaceCache.TryGetValue(fontSource.FontFamily, out var typeface))
                {
                    var font = new Xamarin.Forms.Font();
                    // HACK: there is no way to set the font family of Font
                    // and the only public extension method to get the typeface is thorugh a Xamarin.Forms.Font
                    // so we use reflection here to set the font family and take advantage of ToTypeface method
                    // Also, we need to box the font in order to use reflection to set the property because it's a struct
                    object fontBoxed = font;
                    var pinfo = typeof(Xamarin.Forms.Font)
                        .GetProperty(nameof(Xamarin.Forms.Font.FontFamily));
                    pinfo.SetValue(fontBoxed, fontSource.FontFamily, null);
                    typeface = ((Xamarin.Forms.Font)fontBoxed).ToTypeface();
                    _fontFamilyTypefaceCache.Add(fontSource.FontFamily, typeface);
                }

                paint.SetTypeface(typeface);
            }

            int alpha = Math.Abs(((int)((itemView.TranslationX / itemView.Width) * 510)));
            paint.Alpha = Math.Min(alpha, 255);

            return paint;
        }

        public override int GetSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
        {
            if (viewHolder is TemplatedItemViewHolder templatedViewHolder
                &&
                _swipeableItem.CanSwipe(_viewHolderToTItem(viewHolder)))
            {
                return base.GetSwipeDirs(recyclerView, viewHolder);
            }
            return 0;
        }

        private void ClearCanvas(Canvas c, float left, float top, float right, float bottom)
        {
            if (c != null)
                c.DrawRect(left, top, right, bottom, _clearPaint);
        }

        public override void OnSwiped(RecyclerView.ViewHolder viewHolder, int direction)
        {
            OnSwipedCommand?.Execute(viewHolder);
        }
    }
}
