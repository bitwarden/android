using System;
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
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
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
                Padding = new Thickness(15, 10),
                VerticalOptions = LayoutOptions.CenterAndExpand
            };

            stackLayout.Children.Add(Label);
            stackLayout.Children.Add(Picker);

            if(Device.RuntimePlatform == Device.Android)
            {
                stackLayout.Spacing = 0;
            }
            Picker.AdjustMarginsForDevice();
            stackLayout.AdjustPaddingForDevice();

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedPicker Picker { get; private set; }

        private void FormPickerCell_Tapped(object sender, EventArgs e)
        {
            Picker.Focus();
        }

        public void InitEvents()
        {
            Tapped += FormPickerCell_Tapped;
        }

        public void Dispose()
        {
            Tapped -= FormPickerCell_Tapped;
        }
    }
}
