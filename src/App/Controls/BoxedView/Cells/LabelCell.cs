using Xamarin.Forms;

namespace Bit.App.Controls.BoxedView
{
    public class LabelCell : BaseCell
    {
        public static BindableProperty ValueTextProperty = BindableProperty.Create(
            nameof(ValueText), typeof(string), typeof(LabelCell), default(string),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ValueTextColorProperty = BindableProperty.Create(
            nameof(ValueTextColor), typeof(Color), typeof(LabelCell), Color.Black,
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ValueTextFontSizeProperty = BindableProperty.Create(
            nameof(ValueTextFontSize), typeof(double), typeof(LabelCell), 18.0,
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
    }
}
