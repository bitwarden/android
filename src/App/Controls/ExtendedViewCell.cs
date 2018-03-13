using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedViewCell : ViewCell
    {
        public static readonly BindableProperty BackgroundColorProperty =
            BindableProperty.Create(nameof(BackgroundColor), typeof(Color), typeof(ExtendedViewCell), Color.White);

        public static readonly BindableProperty ShowDisclousureProperty =
            BindableProperty.Create(nameof(ShowDisclousure), typeof(bool), typeof(ExtendedViewCell), false);

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
    }
}
