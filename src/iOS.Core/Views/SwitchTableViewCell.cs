using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public class SwitchTableViewCell : ExtendedUITableViewCell
    {
        public SwitchTableViewCell(string labelName)
            : base(UITableViewCellStyle.Default, nameof(SwitchTableViewCell))
        {
            TextLabel.Text = labelName;
            TextLabel.TextColor = ThemeHelpers.TextColor;
            if (!ThemeHelpers.LightTheme)
            {
                Switch.TintColor = ThemeHelpers.MutedColor;
            }
            Switch.OnTintColor = ThemeHelpers.PrimaryColor;
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
