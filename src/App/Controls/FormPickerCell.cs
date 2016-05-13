using System;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormPickerCell : ViewCell
    {
        public FormPickerCell(string labelText, string[] pickerItems)
        {
            Label = new Label
            {
                Text = labelText,
                FontSize = 14,
                TextColor = Color.FromHex("777777")
            };

            Picker = new ExtendedPicker
            {
                HasBorder = false
            };

            foreach(var item in pickerItems)
            {
                Picker.Items.Add(item);
            }
            Picker.SelectedIndex = 0;

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15, 15, 15, 0),
                BackgroundColor = Color.White
            };

            stackLayout.Children.Add(Label);
            stackLayout.Children.Add(Picker);

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedPicker Picker { get; private set; }
    }
}
