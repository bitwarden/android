using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedPicker : Picker
    {
        // TODO: text color

        public static readonly BindableProperty HasBorderProperty =
            BindableProperty.Create(nameof(HasBorder), typeof(bool), typeof(ExtendedEntry), true);

        public bool HasBorder
        {
            get { return (bool)GetValue(HasBorderProperty); }
            set { SetValue(HasBorderProperty, value); }
        }
    }
}
