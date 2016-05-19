using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedSwitchCell : SwitchCell
    {
        public static readonly BindableProperty BackgroundColorProperty =
            BindableProperty.Create(nameof(BackgroundColor), typeof(Color), typeof(ExtendedSwitchCell), Color.White);

        public Color BackgroundColor
        {
            get { return (Color)GetValue(BackgroundColorProperty); }
            set { SetValue(BackgroundColorProperty, value); }
        }
    }
}
