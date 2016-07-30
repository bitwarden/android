using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class SliderTableViewCell : UITableViewCell
    {
        private string _detailRightSpace = "\t";
        private int _value;

        public SliderTableViewCell(string labelName, int value, int min, int max)
            : base(UITableViewCellStyle.Value1, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            DetailTextLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            Slider = new UISlider
            {
                MinValue = min,
                MaxValue = max,
                TintColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f),
                Frame = new CoreGraphics.CGRect(0, 0, 180, 30)
            };
            Slider.ValueChanged += Slider_ValueChanged;
            Value = value;

            AccessoryView = Slider;
        }

        private void Slider_ValueChanged(object sender, EventArgs e)
        {
            var newValue = Convert.ToInt32(Math.Round(Slider.Value, 0));
            bool valueChanged = newValue != Value;

            Value = newValue;

            if(valueChanged)
            {
                ValueChanged?.Invoke(this, null);
            }
        }

        public UISlider Slider { get; set; }
        public int Value
        {
            get { return _value; }
            set
            {
                _value = value;
                Slider.Value = value;
                DetailTextLabel.Text = string.Concat(value.ToString(), _detailRightSpace);
            }
        }
        public event EventHandler ValueChanged;
    }
}
