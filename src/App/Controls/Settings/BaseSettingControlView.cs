using System.ComponentModel;
using System.Runtime.CompilerServices;
using Bit.App.Utilities;
using Xamarin.Forms;
using static PInvoke.BCrypt;

namespace Bit.App.Controls
{
    public class BaseSettingItemView : ContentView
    {
        public static readonly BindableProperty TitleProperty = BindableProperty.Create(
            nameof(Title), typeof(string), typeof(SwitchItemView), null);

        public static readonly BindableProperty SubtitleProperty = BindableProperty.Create(
            nameof(Subtitle), typeof(string), typeof(SwitchItemView), null);

        public static readonly BindableProperty ItemEnabledProperty = BindableProperty.Create(
            nameof(ItemEnabled), typeof(bool), typeof(SwitchItemView), true);

        public static readonly BindableProperty ItemTextColorProperty = BindableProperty.Create(
            nameof(ItemTextColor), typeof(Color), typeof(SwitchItemView));

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

        public bool ItemEnabled
        {
            get { return (bool)GetValue(ItemEnabledProperty); }
            set {SetValue(ItemEnabledProperty, value);}
        }

        public Color ItemTextColor
        {
            get { return (Color)GetValue(ItemTextColorProperty); }
            set { SetValue(ItemTextColorProperty, value); }
        }

        protected override void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == nameof(ItemEnabled))
            {
                ItemTextColor = ItemEnabled ? ThemeManager.GetResourceColor("TextColor") :
            ThemeManager.GetResourceColor("MutedColor");
            }
        }
    }
}
