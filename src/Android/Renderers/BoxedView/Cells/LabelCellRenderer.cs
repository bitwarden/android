using Android.Content;
using Android.Runtime;
using Android.Text;
using Android.Views;
using Android.Widget;
using Bit.App.Controls.BoxedView;
using Bit.Droid.Renderers.BoxedView;
using System.ComponentModel;
using Xamarin.Forms;
using Xamarin.Forms.Platform.Android;

[assembly: ExportRenderer(typeof(LabelCell), typeof(LabelCellRenderer))]
namespace Bit.Droid.Renderers.BoxedView
{
    [Preserve(AllMembers = true)]
    public class LabelCellRenderer : BaseCellRenderer<LabelCellView>
    { }

    [Preserve(AllMembers = true)]
    public class LabelCellView : BaseCellView
    {
        private bool _debugWithColors = false;
        private TextView _valueLabel;

        public LabelCellView(Context context, Cell cell)
            : base(context, cell)
        {
            _valueLabel = new TextView(context)
            {
                Ellipsize = TextUtils.TruncateAt.End,
                Gravity = GravityFlags.Left,
            };
            _valueLabel.SetSingleLine(true);

            using(var lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WrapContent,
                ViewGroup.LayoutParams.WrapContent))
            {
                CellContent.AddView(_valueLabel, lParams);
            }

            if(_debugWithColors)
            {
                _valueLabel.Background = _Context.GetDrawable(Android.Resource.Color.HoloRedLight);
            }
        }

        private LabelCell LabelCell => Cell as LabelCell;

        public override void CellPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            base.CellPropertyChanged(sender, e);
            if(e.PropertyName == LabelCell.ValueTextProperty.PropertyName)
            {
                UpdateValueText();
            }
            else if(e.PropertyName == LabelCell.ValueTextFontSizeProperty.PropertyName)
            {
                UpdateValueTextFontSize();
            }
            else if(e.PropertyName == LabelCell.ValueTextColorProperty.PropertyName)
            {
                UpdateValueTextColor();
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
                UpdateValueTextFontSize();
            }
        }

        public override void UpdateCell()
        {
            base.UpdateCell();
            UpdateValueText();
            UpdateValueTextColor();
            UpdateValueTextFontSize();
        }

        protected void UpdateValueText()
        {
            _valueLabel.Text = LabelCell.ValueText;
        }

        private void UpdateValueTextFontSize()
        {
            if(LabelCell.ValueTextFontSize > 0)
            {
                _valueLabel.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)LabelCell.ValueTextFontSize);
            }
            else if(CellParent != null)
            {
                _valueLabel.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)CellParent.CellValueTextFontSize);
            }
            Invalidate();
        }

        private void UpdateValueTextColor()
        {
            if(LabelCell.ValueTextColor != Color.Default)
            {
                _valueLabel.SetTextColor(LabelCell.ValueTextColor.ToAndroid());
            }
            else if(CellParent != null && CellParent.CellValueTextColor != Color.Default)
            {
                _valueLabel.SetTextColor(CellParent.CellValueTextColor.ToAndroid());
            }
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                _valueLabel?.Dispose();
                _valueLabel = null;
            }
            base.Dispose(disposing);
        }
    }
}
