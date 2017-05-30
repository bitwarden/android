using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedTextCell : TextCell
    {
        public ExtendedTextCell()
        {
            if(Device.RuntimePlatform == Device.Android)
            {
                TextColor = Color.Black;
            }

            DetailColor = Color.FromHex("777777");
        }

        public static readonly BindableProperty BackgroundColorProperty =
            BindableProperty.Create(nameof(BackgroundColor), typeof(Color), typeof(ExtendedTextCell), Color.White);

        public static readonly BindableProperty ShowDisclousureProperty =
            BindableProperty.Create(nameof(ShowDisclousure), typeof(bool), typeof(ExtendedTextCell), false);

        public static readonly BindableProperty DisclousureImageProperty =
            BindableProperty.Create(nameof(DisclousureImage), typeof(string), typeof(ExtendedTextCell), string.Empty);

        public Color BackgroundColor
        {
            get { return (Color)GetValue(BackgroundColorProperty); }
            set { SetValue(BackgroundColorProperty, value); }
        }

        public bool ShowDisclousure
        {
            get { return (bool)GetValue(ShowDisclousureProperty); }
            set { SetValue(ShowDisclousureProperty, value); }
        }

        public string DisclousureImage
        {
            get { return (string)GetValue(DisclousureImageProperty); }
            set { SetValue(DisclousureImageProperty, value); }
        }

        public LineBreakMode DetailLineBreakMode { get; set; } = LineBreakMode.TailTruncation;

        public event EventHandler DisclousureTapped;

        public void OnDisclousureTapped()
        {
            if(DisclousureTapped == null)
            {
                OnTapped();
                return;
            }

            DisclousureTapped.Invoke(this, EventArgs.Empty);
        }
    }
}
