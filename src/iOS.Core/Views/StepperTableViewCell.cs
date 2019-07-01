using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class StepperTableViewCell : ExtendedUITableViewCell
    {
        // Give some space to the right of the detail in between the spacer.
        // This is a bit of a hack, but I did not see a way to specify a margin on the
        // detaul DetailTextLabel or AccessoryView
        private string _detailRightSpace = "\t";
        private int _value;

        public StepperTableViewCell(string labelName, int value, int min, int max, int increment)
            : base(UITableViewCellStyle.Value1, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            TextLabel.TextColor = ThemeHelpers.TextColor;
            DetailTextLabel.TextColor = ThemeHelpers.MutedColor;

            Stepper = new UIStepper
            {
                TintColor = ThemeHelpers.MutedColor,
                MinimumValue = min,
                MaximumValue = max
            };
            Stepper.ValueChanged += Stepper_ValueChanged;
            Value = value;

            AccessoryView = Stepper;
        }

        private void Stepper_ValueChanged(object sender, EventArgs e)
        {
            Value = Convert.ToInt32(Stepper.Value);
            ValueChanged?.Invoke(this, null);
        }

        public UIStepper Stepper { get; private set; }
        public int Value
        {
            get { return _value; }
            set
            {
                _value = value;
                Stepper.Value = value;
                DetailTextLabel.Text = string.Concat(value.ToString(), _detailRightSpace);
            }
        }
        public event EventHandler ValueChanged;
    }
}
