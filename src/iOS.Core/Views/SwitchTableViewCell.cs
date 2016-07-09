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
        }

        public UISwitch Switch { get; set; } = new UISwitch();
    }
}
