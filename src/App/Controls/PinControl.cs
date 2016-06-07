using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class PinControl
    {
        private Action _pinEnteredAction;
        private Action _confirmPinEnteredAction;

        public PinControl(Action pinEnteredAction, Action confirmPinEnteredAction = null)
        {
            _pinEnteredAction = pinEnteredAction;
            _confirmPinEnteredAction = confirmPinEnteredAction;

            Label = new Label
            {
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = 30,
                TextColor = Color.FromHex("333333"),
                FontFamily = "Courier"
            };

            Entry = new ExtendedEntry
            {
                Keyboard = Keyboard.Numeric,
                MaxLength = 4,
                Margin = new Thickness(0, int.MaxValue, 0, 0)
            };
            Entry.TextChanged += Entry_TextChanged;

            ConfirmEntry = new ExtendedEntry
            {
                Keyboard = Keyboard.Numeric,
                MaxLength = 4,
                Margin = new Thickness(0, int.MaxValue, 0, 0)
            };
            Entry.TextChanged += ConfirmEntry_TextChanged;
        }

        private void Entry_TextChanged(object sender, TextChangedEventArgs e)
        {
            if(e.NewTextValue.Length >= 4 && _pinEnteredAction != null)
            {
                _pinEnteredAction();
            }
        }

        private void ConfirmEntry_TextChanged(object sender, TextChangedEventArgs e)
        {
            if(e.NewTextValue.Length >= 4 && _confirmPinEnteredAction != null)
            {
                _confirmPinEnteredAction();
            }
        }

        public Label Label { get; set; }
        public ExtendedEntry Entry { get; set; }
        public ExtendedEntry ConfirmEntry { get; set; }
    }
}
