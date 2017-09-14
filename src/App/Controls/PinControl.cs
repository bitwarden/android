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
                FontFamily = Helpers.OnPlatform(iOS: "Menlo-Regular", Android: "monospace", WinPhone: "Courier")
            };

            Entry = new ExtendedEntry
            {
                Keyboard = Keyboard.Numeric,
                MaxLength = 4,
                Margin = new Thickness(0, int.MaxValue, 0, 0)
            };

            if(Device.RuntimePlatform == Device.Android)
            {
                Label.TextColor = Color.Black;
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
