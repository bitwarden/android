using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class SwitchTableViewCell : UITableViewCell
    {
        public SwitchTableViewCell(string labelName)
            : base(UITableViewCellStyle.Default, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            AccessoryView = Switch;

            Switch.ValueChanged += Switch_ValueChanged;
        }

        private void Switch_ValueChanged(object sender, EventArgs e)
        {
            ValueChanged?.Invoke(this, null);
        }

        public UISwitch Switch { get; set; } = new UISwitch();
        public event EventHandler ValueChanged;
    }
}
