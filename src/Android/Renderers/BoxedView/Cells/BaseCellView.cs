using Android.Content;
using Android.Graphics.Drawables;
using Android.Runtime;
using Android.Util;
using Android.Views;
using Android.Widget;
using Bit.App.Controls.BoxedView;
using System;
using System.ComponentModel;
using System.Threading;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;
using ARelativeLayout = Android.Widget.RelativeLayout;

namespace Bit.Droid.Renderers.BoxedView
{
    [Preserve(AllMembers = true)]
    public class BaseCellView : ARelativeLayout, INativeElementView
    {
        private CancellationTokenSource _iconTokenSource;
        private Android.Graphics.Color _defaultTextColor;
        private ColorDrawable _backgroundColor;
        private ColorDrawable _selectedColor;
        private RippleDrawable _ripple;
        private float _defaultFontSize;

        protected Context _Context;

        public BaseCellView(Context context, Cell cell)
            : base(context)
        {
            _Context = context;
            Cell = cell;
            CreateContentView();
        }

        public Cell Cell { get; set; }
        public Element Element => Cell;
        protected BaseCell CellBase => Cell as BaseCell;
        public App.Controls.BoxedView.BoxedView CellParent => Cell.Parent as App.Controls.BoxedView.BoxedView;
        public TextView CellTitle { get; set; }
        public LinearLayout CellTitleContent { get; set; }
        public LinearLayout CellContent { get; set; }
        public LinearLayout CellAccessory { get; set; }

        private void CreateContentView()
        {
            var contentView = (_Context as FormsAppCompatActivity)
                .LayoutInflater
                .Inflate(Resource.Layout.CellBaseView, this, true);

            contentView.LayoutParameters = new ViewGroup.LayoutParams(-1, -1);

            CellTitle = contentView.FindViewById<TextView>(Resource.Id.CellTitle);
            CellContent = contentView.FindViewById<LinearLayout>(Resource.Id.CellContent);
            CellTitleContent = contentView.FindViewById<LinearLayout>(Resource.Id.CellTitleContent);
            CellAccessory = contentView.FindViewById<LinearLayout>(Resource.Id.CellAccessory);

            _backgroundColor = new ColorDrawable();
            _selectedColor = new ColorDrawable(Android.Graphics.Color.Argb(125, 180, 180, 180));

            var sel = new StateListDrawable();

            sel.AddState(new int[] { Android.Resource.Attribute.StateSelected }, _selectedColor);
            sel.AddState(new int[] { -Android.Resource.Attribute.StateSelected }, _backgroundColor);
            sel.SetExitFadeDuration(250);
            sel.SetEnterFadeDuration(250);

            var rippleColor = Android.Graphics.Color.Rgb(180, 180, 180);
            if(CellParent.SelectedColor != Color.Default)
            {
                rippleColor = CellParent.SelectedColor.ToAndroid();
            }
            _ripple = RendererUtils.CreateRipple(rippleColor, sel);
            Background = _ripple;

            _defaultTextColor = new Android.Graphics.Color(CellTitle.CurrentTextColor);
            _defaultFontSize = CellTitle.TextSize;
        }

        public virtual void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if(e.PropertyName == BaseCell.TitleProperty.PropertyName)
            {
                UpdateTitleText();
            }
            else if(e.PropertyName == BaseCell.TitleColorProperty.PropertyName)
            {
                UpdateTitleColor();
            }
            else if(e.PropertyName == BaseCell.TitleFontSizeProperty.PropertyName)
            {
                UpdateTitleFontSize();
            }
            else if(e.PropertyName == BaseCell.BackgroundColorProperty.PropertyName)
            {
                UpdateBackgroundColor();
            }
            else if(e.PropertyName == Cell.IsEnabledProperty.PropertyName)
            {
                UpdateIsEnabled();
            }
        }

        public virtual void ParentPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            // Avoid running the vain process when popping a page.
            if((sender as BindableObject)?.BindingContext == null)
            {
                return;
            }

            if(e.PropertyName == App.Controls.BoxedView.BoxedView.CellTitleColorProperty.PropertyName)
            {
                UpdateTitleColor();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.CellTitleFontSizeProperty.PropertyName)
            {
                UpdateWithForceLayout(UpdateTitleFontSize);
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.CellBackgroundColorProperty.PropertyName)
            {
                UpdateBackgroundColor();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.SelectedColorProperty.PropertyName)
            {
                UpdateWithForceLayout(UpdateSelectedColor);
            }
        }

        public virtual void SectionPropertyChanged(object sender, PropertyChangedEventArgs e)
        { }

        public virtual void RowSelected(BoxedViewRecyclerAdapter adapter, int position)
        { }

        protected void UpdateWithForceLayout(Action updateAction)
        {
            updateAction();
            Invalidate();
        }

        public virtual void UpdateCell()
        {
            UpdateBackgroundColor();
            UpdateSelectedColor();
            UpdateTitleText();
            UpdateTitleColor();
            UpdateTitleFontSize();

            UpdateIsEnabled();

            Invalidate();
        }

        private void UpdateBackgroundColor()
        {
            Selected = false;
            if(CellBase.BackgroundColor != Color.Default)
            {
                _backgroundColor.Color = CellBase.BackgroundColor.ToAndroid();
            }
            else if(CellParent != null && CellParent.CellBackgroundColor != Color.Default)
            {
                _backgroundColor.Color = CellParent.CellBackgroundColor.ToAndroid();
            }
            else
            {
                _backgroundColor.Color = Android.Graphics.Color.Transparent;
            }
        }

        private void UpdateSelectedColor()
        {
            if(CellParent != null && CellParent.SelectedColor != Color.Default)
            {
                _selectedColor.Color = CellParent.SelectedColor.MultiplyAlpha(0.5).ToAndroid();
                _ripple.SetColor(RendererUtils.GetPressedColorSelector(CellParent.SelectedColor.ToAndroid()));
            }
            else
            {
                _selectedColor.Color = Android.Graphics.Color.Argb(125, 180, 180, 180);
                _ripple.SetColor(RendererUtils.GetPressedColorSelector(Android.Graphics.Color.Rgb(180, 180, 180)));
            }
        }

        private void UpdateTitleText()
        {
            CellTitle.Text = CellBase.Title;
            // Hide TextView right padding when TextView.Text empty.
            CellTitle.Visibility = string.IsNullOrEmpty(CellTitle.Text) ? ViewStates.Gone : ViewStates.Visible;
        }

        private void UpdateTitleColor()
        {
            if(CellBase.TitleColor != Color.Default)
            {
                CellTitle.SetTextColor(CellBase.TitleColor.ToAndroid());
            }
            else if(CellParent != null && CellParent.CellTitleColor != Color.Default)
            {
                CellTitle.SetTextColor(CellParent.CellTitleColor.ToAndroid());
            }
            else
            {
                CellTitle.SetTextColor(_defaultTextColor);
            }
        }

        private void UpdateTitleFontSize()
        {
            if(CellBase.TitleFontSize > 0)
            {
                CellTitle.SetTextSize(ComplexUnitType.Sp, (float)CellBase.TitleFontSize);
            }
            else if(CellParent != null)
            {
                CellTitle.SetTextSize(ComplexUnitType.Sp, (float)CellParent.CellTitleFontSize);
            }
            else
            {
                CellTitle.SetTextSize(ComplexUnitType.Sp, _defaultFontSize);
            }
        }

        protected virtual void UpdateIsEnabled()
        {
            SetEnabledAppearance(CellBase.IsEnabled);
        }

        protected virtual void SetEnabledAppearance(bool isEnabled)
        {
            if(isEnabled)
            {
                Focusable = false;
                DescendantFocusability = DescendantFocusability.AfterDescendants;
                CellTitle.Alpha = 1f;
            }
            else
            {
                // not to invoke a ripple effect and not to selected
                Focusable = true;
                DescendantFocusability = DescendantFocusability.BlockDescendants;
                // to turn like disabled
                CellTitle.Alpha = 0.3f;
            }
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                CellBase.PropertyChanged -= CellPropertyChanged;
                CellParent.PropertyChanged -= ParentPropertyChanged;

                if(CellBase.Section != null)
                {
                    CellBase.Section.PropertyChanged -= SectionPropertyChanged;
                    CellBase.Section = null;
                }

                CellTitle?.Dispose();
                CellTitle = null;
                CellTitleContent?.Dispose();
                CellTitleContent = null;
                CellAccessory?.Dispose();
                CellAccessory = null;
                Cell = null;

                _iconTokenSource?.Dispose();
                _iconTokenSource = null;
                _Context = null;

                _backgroundColor?.Dispose();
                _backgroundColor = null;
                _selectedColor?.Dispose();
                _selectedColor = null;
                _ripple?.Dispose();
                _ripple = null;

                Background?.Dispose();
                Background = null;
            }
            base.Dispose(disposing);
        }
    }
}
