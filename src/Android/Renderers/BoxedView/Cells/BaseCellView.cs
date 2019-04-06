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
        private bool _debugWithColors = false;
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
        public LinearLayout CellButtonContent { get; set; }
        public Android.Widget.ImageButton CellButton1 { get; set; }
        public Android.Widget.ImageButton CellButton2 { get; set; }
        public Android.Widget.ImageButton CellButton3 { get; set; }
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
            CellButtonContent = contentView.FindViewById<LinearLayout>(Resource.Id.CellButtonContent);
            CellButton1 = contentView.FindViewById<Android.Widget.ImageButton>(Resource.Id.CellButton1);
            CellButton1.Click += CellButton1_Click;
            CellButton2 = contentView.FindViewById<Android.Widget.ImageButton>(Resource.Id.CellButton2);
            CellButton2.Click += CellButton2_Click;
            CellButton3 = contentView.FindViewById<Android.Widget.ImageButton>(Resource.Id.CellButton3);
            CellButton3.Click += CellButton3_Click;
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
            
            if(_debugWithColors)
            {
                contentView.Background = _Context.GetDrawable(Android.Resource.Color.HoloGreenLight);
                CellContent.Background = _Context.GetDrawable(Android.Resource.Color.HoloOrangeLight);
                CellButtonContent.Background = _Context.GetDrawable(Android.Resource.Color.HoloOrangeDark);
                CellTitle.Background = _Context.GetDrawable(Android.Resource.Color.HoloBlueLight);
            }
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
            else if(e.PropertyName == BaseCell.Button1IconProperty.PropertyName)
            {
                UpdateButtonIcon(CellButton1, CellBase.Button1Icon);
            }
            else if(e.PropertyName == BaseCell.Button2IconProperty.PropertyName)
            {
                UpdateButtonIcon(CellButton2, CellBase.Button2Icon);
            }
            else if(e.PropertyName == BaseCell.Button3IconProperty.PropertyName)
            {
                UpdateButtonIcon(CellButton3, CellBase.Button3Icon);
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
            UpdateButtonIcon(CellButton1, CellBase.Button1Icon);
            UpdateButtonIcon(CellButton2, CellBase.Button2Icon);
            UpdateButtonIcon(CellButton3, CellBase.Button3Icon);
            UpdateBackgroundColor();
            UpdateSelectedColor();
            UpdateTitleText();
            UpdateTitleColor();
            UpdateTitleFontSize();

            UpdateIsEnabled();

            Invalidate();
        }

        private void UpdateButtonIcon(Android.Widget.ImageButton cellButton, string icon)
        {
            if(string.IsNullOrWhiteSpace(icon))
            {
                cellButton.Visibility = ViewStates.Gone;
            }
            else
            {
                cellButton.SetImageDrawable(_Context.GetDrawable(icon));
                cellButton.SetImageDrawable(_Context.GetDrawable(icon));
                cellButton.Visibility = ViewStates.Visible;
            }
        }

        private void CellButton1_Click(object sender, EventArgs e)
        {
            if(CellBase.Button1Command?.CanExecute(CellBase.Button1CommandParameter) ?? false)
            {
                CellBase.Button1Command.Execute(CellBase.Button1CommandParameter);
            }
        }

        private void CellButton2_Click(object sender, EventArgs e)
        {
            if(CellBase.Button2Command?.CanExecute(CellBase.Button2CommandParameter) ?? false)
            {
                CellBase.Button2Command.Execute(CellBase.Button2CommandParameter);
            }
        }

        private void CellButton3_Click(object sender, EventArgs e)
        {
            if(CellBase.Button3Command?.CanExecute(CellBase.Button3CommandParameter) ?? false)
            {
                CellBase.Button3Command.Execute(CellBase.Button3CommandParameter);
            }
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
                CellButton1.Click -= CellButton1_Click;
                CellButton2.Click -= CellButton2_Click;
                CellButton3.Click -= CellButton3_Click;

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
                CellButton1?.Dispose();
                CellButton1 = null;
                CellButton2?.Dispose();
                CellButton2 = null;
                CellButton3?.Dispose();
                CellButton3 = null;
                CellButtonContent?.Dispose();
                CellButtonContent = null;
                CellContent?.Dispose();
                CellContent = null;
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
