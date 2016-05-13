using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormEntryCell : ViewCell
    {
        public FormEntryCell(string labelText, Keyboard entryKeyboard = null, bool IsPassword = false)
        {
            Label = new Label
            {
                Text = labelText,
                FontSize = 14,
                TextColor = Color.FromHex("777777")
            };

            Entry = new ExtendedEntry
            {
                Keyboard = entryKeyboard,
                HasBorder = false
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15, 15, 15, 0),
                BackgroundColor = Color.White
            };

            stackLayout.Children.Add(Label);
            stackLayout.Children.Add(Entry);

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedEntry Entry { get; private set; }
    }
}
