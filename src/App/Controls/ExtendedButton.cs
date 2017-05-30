using Bit.App.Enums;
using Bit.App.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedButton : Button
    {
        public static readonly BindableProperty PaddingProperty =
            BindableProperty.Create(nameof(Padding), typeof(Thickness), typeof(ExtendedButton), default(Thickness));
        public static readonly BindableProperty UppercaseProperty =
            BindableProperty.Create(nameof(Uppercase), typeof(bool), typeof(ExtendedButton),
                Helpers.OnPlatform(iOS: false, Android: true, WinPhone: false));

        public Thickness Padding
        {
            get { return (Thickness)GetValue(PaddingProperty); }
            set { SetValue(PaddingProperty, value); }
        }

        public bool Uppercase
        {
            get { return (bool)GetValue(UppercaseProperty); }
            set { SetValue(UppercaseProperty, value); }
        }
    }
}
