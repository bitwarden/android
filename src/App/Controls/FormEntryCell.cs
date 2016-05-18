using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormEntryCell : ExtendedViewCell
    {
        public FormEntryCell(string labelText, Keyboard entryKeyboard = null, bool IsPassword = false)
        {
            Label = new Label
            {
                Text = labelText,
                FontSize = 14,
                TextColor = Color.FromHex("777777"),
                VerticalOptions = LayoutOptions.Start
            };

            Entry = new ExtendedEntry
            {
                Keyboard = entryKeyboard,
                HasBorder = false,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Margin = new Thickness(0, 5, 0, 0)
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15)
            };

            stackLayout.Children.Add(Label);
            stackLayout.Children.Add(Entry);

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedEntry Entry { get; private set; }
    }
}
