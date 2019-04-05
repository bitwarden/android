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
        public LabelCellView(Context context, Cell cell)
            : base(context, cell)
        {
            ValueLabel = new TextView(context);
            ValueLabel.SetSingleLine(true);
            ValueLabel.Ellipsize = TextUtils.TruncateAt.End;
            ValueLabel.Gravity = GravityFlags.Right;

            var textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WrapContent,
                ViewGroup.LayoutParams.WrapContent);
            using(textParams)
            {
                ContentStack.AddView(ValueLabel, textParams);
            }
        }

        private LabelCell _LabelCell => Cell as LabelCell;

        public TextView ValueLabel { get; set; }

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

        protected override void SetEnabledAppearance(bool isEnabled)
        {
            if(isEnabled)
            {
                ValueLabel.Alpha = 1f;
            }
            else
            {
                ValueLabel.Alpha = 0.3f;
            }
            base.SetEnabledAppearance(isEnabled);
        }

        protected void UpdateValueText()
        {
            ValueLabel.Text = _LabelCell.ValueText;
        }

        private void UpdateValueTextFontSize()
        {
            if(_LabelCell.ValueTextFontSize > 0)
            {
                ValueLabel.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)_LabelCell.ValueTextFontSize);
            }
            else if(CellParent != null)
            {
                ValueLabel.SetTextSize(Android.Util.ComplexUnitType.Sp, (float)CellParent.CellValueTextFontSize);
            }
            Invalidate();
        }

        private void UpdateValueTextColor()
        {
            if(_LabelCell.ValueTextColor != Color.Default)
            {
                ValueLabel.SetTextColor(_LabelCell.ValueTextColor.ToAndroid());
            }
            else if(CellParent != null && CellParent.CellValueTextColor != Color.Default)
            {
                ValueLabel.SetTextColor(CellParent.CellValueTextColor.ToAndroid());
            }
        }

        protected override void Dispose(bool disposing)
        {
            if(disposing)
            {
                ValueLabel?.Dispose();
                ValueLabel = null;
            }
            base.Dispose(disposing);
        }
    }
}
