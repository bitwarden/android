using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormEntryCell : ExtendedViewCell
    {
        public FormEntryCell(string labelText, Keyboard entryKeyboard = null, bool IsPassword = false, VisualElement nextElement = null, bool useLabelAsPlaceholder = false)
        {
            if(!useLabelAsPlaceholder)
            {
                Label = new Label
                {
                    Text = labelText,
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    VerticalOptions = LayoutOptions.Start,
                    Style = (Style)Application.Current.Resources["text-muted"]
                };
            }

            Entry = new ExtendedEntry
            {
                Keyboard = entryKeyboard,
                HasBorder = false,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                IsPassword = IsPassword
            };

            if(useLabelAsPlaceholder)
            {
                Entry.Placeholder = labelText;
            }

            if(nextElement != null)
            {
                Entry.ReturnType = Enums.ReturnType.Next;
                Entry.Completed += (object sender, EventArgs e) => { nextElement.Focus(); };
            }

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15, 10)
            };

            if(!useLabelAsPlaceholder)
            {
                stackLayout.Children.Add(Label);
            }

            stackLayout.Children.Add(Entry);

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedEntry Entry { get; private set; }
    }
}
