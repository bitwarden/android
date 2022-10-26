using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class IconLabel : Label
    {
        public bool ShouldUpdateFontSizeDynamicallyForAccesibility { get; set; }

        public static readonly BindableProperty RemoveFontPaddingProperty = BindableProperty.Create(
            nameof(IncludeFontPadding), typeof(bool?), typeof(IconLabel), defaultValue: true);

        public bool? IncludeFontPadding
        {
            get => GetValue(RemoveFontPaddingProperty) as bool?;
            set => SetValue(RemoveFontPaddingProperty, value);
        }

        public IconLabel()
        {
            switch (Device.RuntimePlatform)
            {
                case Device.iOS:
                    FontFamily = "bwi-font";
                    break;
                case Device.Android:
                    FontFamily = "bwi-font.ttf#bwi-font";
                    break;
            }
        }
    }
}
