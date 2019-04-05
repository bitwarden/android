using System;
using Xamarin.Forms;

namespace Bit.App.Controls.BoxedView
{
    public class EntryCell : BaseCell, IEntryCellController
    {
        public static BindableProperty ValueTextProperty = BindableProperty.Create(
            nameof(ValueText), typeof(string), typeof(EntryCell), default(string),
            defaultBindingMode: BindingMode.TwoWay);
            // propertyChanging: ValueTextPropertyChanging);

        public static BindableProperty ValueTextColorProperty = BindableProperty.Create(
            nameof(ValueTextColor), typeof(Color), typeof(EntryCell), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ValueTextFontSizeProperty = BindableProperty.Create(
            nameof(ValueTextFontSize), typeof(double), typeof(EntryCell), -1.0,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty KeyboardProperty = BindableProperty.Create(
            nameof(Keyboard), typeof(Keyboard), typeof(EntryCell), Keyboard.Default,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty PlaceholderProperty = BindableProperty.Create(
            nameof(Placeholder), typeof(string), typeof(EntryCell), default(string),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty TextAlignmentProperty = BindableProperty.Create(
            nameof(TextAlignment), typeof(TextAlignment), typeof(EntryCell), TextAlignment.Start,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty AccentColorProperty = BindableProperty.Create(
            nameof(AccentColor), typeof(Color), typeof(EntryCell), default(Color),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty IsPasswordProperty = BindableProperty.Create(
            nameof(IsPassword), typeof(bool), typeof(EntryCell), default(bool),
            defaultBindingMode: BindingMode.OneWay);

        public string ValueText
        {
            get => (string)GetValue(ValueTextProperty);
            set => SetValue(ValueTextProperty, value);
        }

        public Color ValueTextColor
        {
            get => (Color)GetValue(ValueTextColorProperty);
            set => SetValue(ValueTextColorProperty, value);
        }

        [TypeConverter(typeof(FontSizeConverter))]
        public double ValueTextFontSize
        {
            get => (double)GetValue(ValueTextFontSizeProperty);
            set => SetValue(ValueTextFontSizeProperty, value);
        }

        public Keyboard Keyboard
        {
            get => (Keyboard)GetValue(KeyboardProperty);
            set => SetValue(KeyboardProperty, value);
        }

        public string Placeholder
        {
            get => (string)GetValue(PlaceholderProperty);
            set => SetValue(PlaceholderProperty, value);
        }

        public TextAlignment TextAlignment
        {
            get => (TextAlignment)GetValue(TextAlignmentProperty);
            set => SetValue(TextAlignmentProperty, value);
        }

        public Color AccentColor
        {
            get => (Color)GetValue(AccentColorProperty);
            set => SetValue(AccentColorProperty, value);
        }

        public bool IsPassword
        {
            get => (bool)GetValue(IsPasswordProperty);
            set => SetValue(IsPasswordProperty, value);
        }

        public event EventHandler Completed;

        public void SendCompleted()
        {
            Completed?.Invoke(this, EventArgs.Empty);
        }

        private static void ValueTextPropertyChanging(BindableObject bindable, object oldValue, object newValue)
        {
            // Check changes
        }
    }
}
