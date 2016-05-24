using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormEntryCell : ExtendedViewCell
    {
        public FormEntryCell(string labelText, Keyboard entryKeyboard = null, bool IsPassword = false, VisualElement nextElement = null)
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
                IsPassword = IsPassword,
                TextColor = Color.FromHex("333333")
            };

            if(nextElement != null)
            {
                Entry.ReturnType = Enums.ReturnType.Next;
                Entry.Completed += (object sender, EventArgs e) => { nextElement.Focus(); };
            }

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
