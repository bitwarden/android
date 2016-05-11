using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedPicker : Picker
    {
        public static readonly BindableProperty HasBorderProperty =
            BindableProperty.Create(nameof(HasBorder), typeof(bool), typeof(ExtendedEntry), true);

        public static readonly BindableProperty HasOnlyBottomBorderProperty =
            BindableProperty.Create(nameof(HasOnlyBottomBorder), typeof(bool), typeof(ExtendedEntry), false);

        public static readonly BindableProperty BottomBorderColorProperty =
            BindableProperty.Create(nameof(BottomBorderColor), typeof(Color), typeof(ExtendedEntry), Color.Default);

        public bool HasBorder
        {
            get { return (bool)GetValue(HasBorderProperty); }
            set { SetValue(HasBorderProperty, value); }
        }

        public bool HasOnlyBottomBorder
        {
            get { return (bool)GetValue(HasOnlyBottomBorderProperty); }
            set { SetValue(HasOnlyBottomBorderProperty, value); }
        }

        public Color BottomBorderColor
        {
            get { return (Color)GetValue(BottomBorderColorProperty); }
            set { SetValue(BottomBorderColorProperty, value); }
        }
    }
}
