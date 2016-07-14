using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class StepperTableViewCell : UITableViewCell
    {
        // Give some space to the right of the detail in between the spacer.
        // This is a bit of a hack, but I did not see a way to specify a margin on the
        // detaul DetailTextLabel or AccessoryView
        private string _detailRightSpace = "\t";

        public StepperTableViewCell(string labelName, double value, double min, double max, double increment)
            : base(UITableViewCellStyle.Value1, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            DetailTextLabel.Text = string.Concat(value.ToString(), _detailRightSpace);
            DetailTextLabel.TextColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f);

            Stepper = new UIStepper
            {
                TintColor = new UIColor(red: 0.47f, green: 0.47f, blue: 0.47f, alpha: 1.0f),
                Value = value,
                MinimumValue = min,
                MaximumValue = max
            };
            Stepper.ValueChanged += Stepper_ValueChanged;

            AccessoryView = Stepper;
        }

        private void Stepper_ValueChanged(object sender, EventArgs e)
        {
            DetailTextLabel.Text = string.Concat(Stepper.Value.ToString(), _detailRightSpace);
        }

        public UIStepper Stepper { get; private set; }
    }
}
