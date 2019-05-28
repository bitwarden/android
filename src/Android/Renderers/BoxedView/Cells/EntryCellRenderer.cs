using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Text;
using Android.Text.Method;
using Android.Views;
using Android.Views.InputMethods;
using Android.Widget;
using Java.Lang;
using System;
using System.ComponentModel;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(Bit.App.Controls.BoxedView.EntryCell),
    typeof(Bit.Droid.Renderers.BoxedView.EntryCellRenderer))]
namespace Bit.Droid.Renderers.BoxedView
{
    [Preserve(AllMembers = true)]
    public class EntryCellRenderer : BaseCellRenderer<EntryCellView>
    { }

    [Preserve(AllMembers = true)]
    public class EntryCellView : BaseCellView, ITextWatcher, Android.Views.View.IOnFocusChangeListener,
        TextView.IOnEditorActionListener
    {
        private bool _debugWithColors = false;
        private CustomEditText _editText;

        public EntryCellView(Context context, Cell cell)
            : base(context, cell)
        {
            _editText = new CustomEditText(context)
            {
                Focusable = true,
                ImeOptions = ImeAction.Done,
                OnFocusChangeListener = this,
                Ellipsize = TextUtils.TruncateAt.End,
                ClearFocusAction = DoneEdit,
                Background = _Context.GetDrawable(Android.Resource.Color.Transparent)
            };
            _editText.SetPadding(0, 0, 0, 0);
            _editText.SetOnEditorActionListener(this);
            _editText.SetSingleLine(true);
            _editText.InputType |= InputTypes.TextFlagNoSuggestions; // Disabled spell check

            Click += EntryCellView_Click;

            using(var lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MatchParent,
                ViewGroup.LayoutParams.WrapContent))
            {
                CellContent.AddView(_editText, lParams);
            }
            
            if(_debugWithColors)
            {
                _editText.Background = _Context.GetDrawable(Android.Resource.Color.HoloRedLight);
            }
        }

        App.Controls.BoxedView.EntryCell _EntryCell => Cell as App.Controls.BoxedView.EntryCell;

        public override void UpdateCell()
        {
            UpdateValueText();
            UpdateValueTextColor();
            UpdateValueTextFontSize();
            UpdateKeyboard();
            UpdatePlaceholder();
            UpdateTextAlignment();
            UpdateIsPassword();
            base.UpdateCell();
        }

        public override void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.CellPropertyChanged(sender, e);
            if(e.PropertyName == App.Controls.BoxedView.EntryCell.ValueTextProperty.PropertyName)
            {
                UpdateValueText();
            }
            else if(e.PropertyName == App.Controls.BoxedView.EntryCell.ValueTextFontSizeProperty.PropertyName)
            {
                UpdateWithForceLayout(UpdateValueTextFontSize);
            }
            else if(e.PropertyName == App.Controls.BoxedView.EntryCell.ValueTextColorProperty.PropertyName)
            {
                UpdateWithForceLayout(UpdateValueTextColor);
            }
            else if(e.PropertyName == App.Controls.BoxedView.EntryCell.KeyboardProperty.PropertyName)
            {
                UpdateKeyboard();
            }
            else if(e.PropertyName == App.Controls.BoxedView.EntryCell.PlaceholderProperty.PropertyName)
            {
                UpdatePlaceholder();
            }
            else if(e.PropertyName == App.Controls.BoxedView.EntryCell.TextAlignmentProperty.PropertyName)
            {
                UpdateTextAlignment();
            }
            else if(e.PropertyName == App.Controls.BoxedView.EntryCell.IsPasswordProperty.PropertyName)
            {
                UpdateIsPassword();
            }
        }

        public override void ParentPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.ParentPropertyChanged(sender, e);
            if(e.PropertyName == App.Controls.BoxedView.BoxedView.CellValueTextColorProperty.PropertyName)
            {
                UpdateValueTextColor();
            }
            else if(e.PropertyName == App.Controls.BoxedView.BoxedView.CellValueTextFontSizeProperty.PropertyName)
            {
                UpdateWithForceLayout(UpdateValueTextFontSize);
            }
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                Click -= EntryCellView_Click;
                _editText.RemoveFromParent();
                _editText.SetOnEditorActionListener(null);
                _editText.RemoveTextChangedListener(this);
                _editText.OnFocusChangeListener = null;
                _editText.ClearFocusAction = null;
                _editText.Dispose();
                _editText = null;
            }
            base.Dispose(disposing);
        }

        private void EntryCellView_Click(object sender, EventArgs e)
        {
            _editText.RequestFocus();
            ShowKeyboard(_editText);
        }

        private void UpdateValueText()
        {
            _editText.RemoveTextChangedListener(this);
            if(_editText.Text != _EntryCell.ValueText)
            {
                _editText.Text = _EntryCell.ValueText;
            }
            _editText.AddTextChangedListener(this);
        }

        private void UpdateValueTextFontSize()
        {
            if(_EntryCell.ValueTextFontSize > 0)
            {
                _editText.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)_EntryCell.ValueTextFontSize);
            }
            else if(CellParent != null)
            {
                _editText.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)CellParent.CellValueTextFontSize);
            }
        }

        private void UpdateValueTextColor()
        {
            if(_EntryCell.ValueTextColor != Color.Default)
            {
                _editText.SetTextColor(_EntryCell.ValueTextColor.ToAndroid());
            }
            else if(CellParent != null && CellParent.CellValueTextColor != Color.Default)
            {
                _editText.SetTextColor(CellParent.CellValueTextColor.ToAndroid());
            }
        }

        private void UpdateKeyboard()
        {
            _editText.InputType = _EntryCell.Keyboard.ToInputType() | InputTypes.TextFlagNoSuggestions;
        }

        private void UpdateIsPassword()
        {
            _editText.TransformationMethod = _EntryCell.IsPassword ? new PasswordTransformationMethod() : null;
        }

        private void UpdatePlaceholder()
        {
            _editText.Hint = _EntryCell.Placeholder;
            _editText.SetHintTextColor(Android.Graphics.Color.Rgb(210, 210, 210));
        }

        private void UpdateTextAlignment()
        {
            _editText.Gravity = _EntryCell.TextAlignment.ToAndroidHorizontal();
        }

        private void DoneEdit()
        {
            var entryCell = (IEntryCellController)Cell;
            entryCell.SendCompleted();
            _editText.ClearFocus();
            ClearFocus();
        }

        private void HideKeyboard(Android.Views.View inputView)
        {
            using(var inputMethodManager = (InputMethodManager)_Context.GetSystemService(Context.InputMethodService))
            {
                IBinder windowToken = inputView.WindowToken;
                if(windowToken != null)
                {
                    inputMethodManager.HideSoftInputFromWindow(windowToken, HideSoftInputFlags.None);
                }
            }
        }

        private void ShowKeyboard(Android.Views.View inputView)
        {
            using(var inputMethodManager = (InputMethodManager)_Context.GetSystemService(Context.InputMethodService))
            {
                inputMethodManager.ShowSoftInput(inputView, ShowFlags.Forced);
                inputMethodManager.ToggleSoftInput(ShowFlags.Forced, HideSoftInputFlags.ImplicitOnly);
            }
        }

        bool TextView.IOnEditorActionListener.OnEditorAction(TextView v, ImeAction actionId, KeyEvent e)
        {
            if(actionId == ImeAction.Done || (actionId == ImeAction.ImeNull && e.KeyCode == Keycode.Enter))
            {
                HideKeyboard(v);
                DoneEdit();
            }
            return true;
        }

        void ITextWatcher.AfterTextChanged(IEditable s)
        { }

        void ITextWatcher.BeforeTextChanged(ICharSequence s, int start, int count, int after)
        { }

        void ITextWatcher.OnTextChanged(ICharSequence s, int start, int before, int count)
        {
            _EntryCell.ValueText = s?.ToString();
        }

        void IOnFocusChangeListener.OnFocusChange(Android.Views.View v, bool hasFocus)
        {
            if(hasFocus)
            {
                // Show underline when on focus.
                _editText.Background.Alpha = 100;
            }
            else
            {
                // Hide underline
                _editText.Background.Alpha = 0;
            }
        }
    }

    [Preserve(AllMembers = true)]
    internal class CustomEditText : EditText
    {
        public CustomEditText(Context context)
            : base(context)
        { }

        public Action ClearFocusAction { get; set; }

        public override bool OnKeyPreIme(Keycode keyCode, KeyEvent e)
        {
            if(keyCode == Keycode.Back && e.Action == KeyEventActions.Up)
            {
                ClearFocus();
                ClearFocusAction?.Invoke();
            }
            return base.OnKeyPreIme(keyCode, e);
        }
    }
}
