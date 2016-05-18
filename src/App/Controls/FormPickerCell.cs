using System;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormPickerCell : ExtendedViewCell
    {
        public FormPickerCell(string labelText, string[] pickerItems)
        {
            Label = new Label
            {
                Text = labelText,
                FontSize = 14,
                TextColor = Color.FromHex("777777"),
                VerticalOptions = LayoutOptions.Start
            };

            Picker = new ExtendedPicker
            {
                HasBorder = false,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Margin = new Thickness(0, 5, 0, 0)
            };

            foreach(var item in pickerItems)
            {
                Picker.Items.Add(item);
            }
            Picker.SelectedIndex = 0;

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15)
            };

            stackLayout.Children.Add(Label);
            stackLayout.Children.Add(Picker);

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedPicker Picker { get; private set; }
    }
}
