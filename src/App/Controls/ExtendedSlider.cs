using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedSlider : Slider
    {
        public static readonly BindableProperty ThumbBorderColorProperty = BindableProperty.Create(
            nameof(ThumbBorderColor), typeof(Color), typeof(ExtendedSlider), Color.FromHex("b5b5b5"));

        public Color ThumbBorderColor
        {
            get => (Color)GetValue(ThumbBorderColorProperty);
            set => SetValue(ThumbBorderColorProperty, value);
        }
    }
}
