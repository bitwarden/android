using Android.Content;
using Android.Runtime;
using Android.Support.V7.Widget;
using Android.Support.V7.Widget.Helper;
using Android.Views;
using Bit.App.Controls.BoxedView;
using Bit.Droid.Renderers.BoxedView;
using System;
using System.ComponentModel;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(BoxedView), typeof(BoxedViewRenderer))]
namespace Bit.Droid.Renderers.BoxedView
{
    [Preserve(AllMembers = true)]
    public class BoxedViewRenderer : ViewRenderer<App.Controls.BoxedView.BoxedView, RecyclerView>
    {
        private Page _parentPage;
        private LinearLayoutManager _layoutManager;
        private ItemTouchHelper _itemTouchhelper;
        private BoxedViewRecyclerAdapter _adapter;
        private BoxedViewSimpleCallback _simpleCallback;

        public BoxedViewRenderer(Context context)
            : base(context)
        {
            AutoPackage = false;
        }

        protected override void OnElementChanged(ElementChangedEventArgs<App.Controls.BoxedView.BoxedView> e)
        {
            base.OnElementChanged(e);
            if(e.NewElement != null)
            {
                var recyclerView = new RecyclerView(Context);
                _layoutManager = new LinearLayoutManager(Context);
                recyclerView.SetLayoutManager(_layoutManager);

                SetNativeControl(recyclerView);

                Control.Focusable = false;
                Control.DescendantFocusability = DescendantFocusability.AfterDescendants;

                UpdateBackgroundColor();
                UpdateRowHeight();

                _adapter = new BoxedViewRecyclerAdapter(Context, e.NewElement, recyclerView);
                Control.SetAdapter(_adapter);

                _simpleCallback = new BoxedViewSimpleCallback(
                    e.NewElement, ItemTouchHelper.Up | ItemTouchHelper.Down, 0);
                _itemTouchhelper = new ItemTouchHelper(_simpleCallback);
                _itemTouchhelper.AttachToRecyclerView(Control);

                Element elm = Element;
                while(elm != null)
                {
                    elm = elm.Parent;
                    if(elm is Page)
                    {
                        break;
                    }
                }

                _parentPage = elm as Page;
                _parentPage.Appearing += ParentPageAppearing;
            }
        }

        private void ParentPageAppearing(object sender, EventArgs e)
        {
            Device.BeginInvokeOnMainThread(() => _adapter.DeselectRow());
        }

        protected override void OnLayout(bool changed, int left, int top, int right, int bottom)
        {
            base.OnLayout(changed, left, top, right, bottom);
            if(!changed)
            {
                return;
            }

            var startPos = _layoutManager.FindFirstCompletelyVisibleItemPosition();
            var endPos = _layoutManager.FindLastCompletelyVisibleItemPosition();

            var totalH = 0;
            for(var i = startPos; i <= endPos; i++)
            {
                var child = _layoutManager.GetChildAt(i);
                if(child == null)
                {
                    return;
                }

                totalH += _layoutManager.GetChildAt(i).Height;
            }
            Element.VisibleContentHeight = Context.FromPixels(Math.Min(totalH, Control.Height));
        }

        protected override void OnElementPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.OnElementPropertyChanged(sender, e);
            if(e.PropertyName == App.Controls.BoxedView.BoxedView.SeparatorColorProperty.PropertyName)
            {
                _adapter.NotifyDataSetChanged();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.BackgroundColorProperty.PropertyName)
            {
                UpdateBackgroundColor();
            }
            else if(e.PropertyName == TableView.RowHeightProperty.PropertyName)
            {
                UpdateRowHeight();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.SelectedColorProperty.PropertyName)
            {
                //_adapter.NotifyDataSetChanged();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.ShowSectionTopBottomBorderProperty.PropertyName)
            {
                _adapter.NotifyDataSetChanged();
            }
            else if(e.PropertyName == TableView.HasUnevenRowsProperty.PropertyName)
            {
                _adapter.NotifyDataSetChanged();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.ScrollToTopProperty.PropertyName)
            {
                UpdateScrollToTop();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.ScrollToBottomProperty.PropertyName)
            {
                UpdateScrollToBottom();
            }
        }

        private void UpdateRowHeight()
        {
            if(Element.RowHeight == -1)
            {
                Element.RowHeight = 60;
            }
            else
            {
                _adapter?.NotifyDataSetChanged();
            }
        }

        private void UpdateScrollToTop()
        {
            if(Element.ScrollToTop)
            {
                _layoutManager.ScrollToPosition(0);
                Element.ScrollToTop = false;
            }
        }

        private void UpdateScrollToBottom()
        {
            if(Element.ScrollToBottom)
            {
                if(_adapter != null)
                {
                    _layoutManager.ScrollToPosition(_adapter.ItemCount - 1);
                }
                Element.ScrollToBottom = false;
            }
        }

        protected new void UpdateBackgroundColor()
        {
            if(Element.BackgroundColor != Color.Default)
            {
                Control.SetBackgroundColor(Element.BackgroundColor.ToAndroid());
            }
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                _parentPage.Appearing -= ParentPageAppearing;
                _adapter?.Dispose();
                _adapter = null;
                _layoutManager?.Dispose();
                _layoutManager = null;
                _simpleCallback?.Dispose();
                _simpleCallback = null;
                _itemTouchhelper?.Dispose();
                _itemTouchhelper = null;
            }
            base.Dispose(disposing);
        }
    }
}
