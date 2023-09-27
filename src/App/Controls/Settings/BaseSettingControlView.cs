using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class BaseSettingItemView : ContentView
    {
        public static readonly BindableProperty TitleProperty = BindableProperty.Create(
            nameof(Title), typeof(string), typeof(SwitchItemView), null, BindingMode.OneWay);

        public static readonly BindableProperty SubtitleProperty = BindableProperty.Create(
            nameof(Subtitle), typeof(string), typeof(SwitchItemView), null, BindingMode.OneWay);

        public string Title
        {
            get { return (string)GetValue(TitleProperty); }
            set { SetValue(TitleProperty, value); }
        }

        public string Subtitle
        {
            get { return (string)GetValue(SubtitleProperty); }
            set { SetValue(SubtitleProperty, value); }
        }
    }
}
