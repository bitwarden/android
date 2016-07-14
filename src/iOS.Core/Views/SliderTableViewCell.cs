using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class SliderTableViewCell : UITableViewCell
    {
        private string _detailRightSpace = "\t";

        public SliderTableViewCell(string labelName, float value, float min, float max)
            : base(UITableViewCellStyle.Value1, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            DetailTextLabel.Text = string.Concat(value.ToString(), _detailRightSpace);
            DetailTextLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            Slider = new UISlider
            {
                MinValue = min,
                MaxValue = max,
                Value = value,
                TintColor = new UIColor(red: 0.24f, green: 0.55f, blue: 0.74f, alpha: 1.0f),
                Frame = new CoreGraphics.CGRect(0, 0, 180, 20)
            };
            Slider.ValueChanged += Slider_ValueChanged;

            AccessoryView = Slider;
        }

        private void Slider_ValueChanged(object sender, EventArgs e)
        {
            Slider.Value = Convert.ToInt32(Math.Round(Slider.Value, 0));
            DetailTextLabel.Text = string.Concat(Slider.Value.ToString(), _detailRightSpace);
        }

        public UISlider Slider { get; set; }
    }
}
