using Bit.App.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class PinControl
    {
        public EventHandler OnPinEntered;

        public PinControl()
        {
            Label = new Label
            {
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = 35,
                FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier")
            };

            Entry = new ExtendedEntry
            {
                Keyboard = Keyboard.Numeric,
                TargetMaxLength = 4,
                HideCursor = true
            };

            Entry.BackgroundColor = Entry.TextColor = Color.Transparent;

            if(Device.RuntimePlatform == Device.Android)
            {
                Label.TextColor = Color.Black;
            }
            else
            {
                Entry.Margin = new Thickness(0, int.MaxValue, 0, 0);
            }
        }

        public Label Label { get; set; }
        public ExtendedEntry Entry { get; set; }

        private void Entry_TextChanged(object sender, TextChangedEventArgs e)
        {
            if(e.NewTextValue.Length >= 4)
            {
                OnPinEntered.Invoke(this, null);
            }
        }

        public void InitEvents()
        {
            Entry.TextChanged += Entry_TextChanged;
        }

        public void Dispose()
        {
            Entry.TextChanged -= Entry_TextChanged;
        }
    }
}
