using System;
using Bit.App.Models.Page;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class PinControl
    {
        private Action _pinEnteredAction;

        public PinControl(Action pinEnteredAction)
        {
            _pinEnteredAction = pinEnteredAction;

            Label = new Label
            {
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = 30,
                TextColor = Color.FromHex("333333"),
                FontFamily = "Courier"
            };
            Label.SetBinding<PinPageModel>(Label.TextProperty, s => s.LabelText);

            Entry = new ExtendedEntry
            {
                Keyboard = Keyboard.Numeric,
                MaxLength = 4,
                Margin = new Thickness(0, int.MaxValue, 0, 0)
            };
            Entry.SetBinding<PinPageModel>(Xamarin.Forms.Entry.TextProperty, s => s.PIN);
            Entry.TextChanged += PinEntry_TextChanged;
        }

        private void PinEntry_TextChanged(object sender, TextChangedEventArgs e)
        {
            if(e.NewTextValue.Length >= 4)
            {
                _pinEnteredAction();
            }
        }

        public Label Label { get; set; }
        public ExtendedEntry Entry { get; set; }
    }
}
