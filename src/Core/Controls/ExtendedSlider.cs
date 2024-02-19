using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class ExtendedSlider : Slider
    {
        public static readonly BindableProperty ThumbBorderColorProperty = BindableProperty.Create(
            nameof(ThumbBorderColor), typeof(Color), typeof(ExtendedSlider), Color.FromArgb("b5b5b5"));

        public Color ThumbBorderColor
        {
            get => (Color)GetValue(ThumbBorderColorProperty);
            set => SetValue(ThumbBorderColorProperty, value);
        }
    }
}
