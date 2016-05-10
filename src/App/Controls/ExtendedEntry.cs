using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedEntry : Entry
    {
        public static readonly BindableProperty HasBorderProperty =
            BindableProperty.Create(nameof(HasBorder), typeof(bool), typeof(ExtendedEntry), true);

        public static readonly BindableProperty HasOnlyBottomBorderProperty =
            BindableProperty.Create(nameof(HasOnlyBottomBorder), typeof(bool), typeof(ExtendedEntry), false);

        public static readonly BindableProperty BorderColorProperty =
            BindableProperty.Create(nameof(BorderColor), typeof(Color), typeof(ExtendedEntry), Color.Default);

        public static readonly BindableProperty PlaceholderTextColorProperty =
            BindableProperty.Create(nameof(PlaceholderTextColor), typeof(Color), typeof(ExtendedEntry), Color.Default);

        public static readonly BindableProperty MaxLengthProperty =
            BindableProperty.Create(nameof(MaxLength), typeof(int), typeof(ExtendedEntry), int.MaxValue);

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

        public Color BorderColor
        {
            get { return (Color)GetValue(BorderColorProperty); }
            set { SetValue(BorderColorProperty, value); }
        }

        public Color PlaceholderTextColor
        {
            get { return (Color)GetValue(PlaceholderTextColorProperty); }
            set { SetValue(PlaceholderTextColorProperty, value); }
        }

        public int MaxLength
        {
            get { return (int)GetValue(MaxLengthProperty); }
            set { SetValue(MaxLengthProperty, value); }
        }
    }
}
