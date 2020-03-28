using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class SliderTableViewCell : ExtendedUITableViewCell
    {
        private string _detailRightSpace = "\t";
        private int _value;

        public SliderTableViewCell(string labelName, int value, int min, int max)
            : base(UITableViewCellStyle.Value1, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            TextLabel.TextColor = ThemeHelpers.TextColor;
            DetailTextLabel.TextColor = ThemeHelpers.MutedColor;

            Slider = new UISlider
            {
                MinValue = min,
                MaxValue = max,
                TintColor = ThemeHelpers.PrimaryColor,
                Frame = new CoreGraphics.CGRect(0, 0, 180, 30),
                BackgroundColor = ThemeHelpers.BackgroundColor
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

            if (valueChanged)
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
