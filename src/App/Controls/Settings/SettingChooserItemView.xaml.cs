using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class SettingChooserItemView : BaseSettingItemView
    {
        public static readonly BindableProperty DisplayValueProperty = BindableProperty.Create(
            nameof(DisplayValue), typeof(string), typeof(SettingChooserItemView), null, BindingMode.OneWay);

        public static readonly BindableProperty ChooseCommandProperty = BindableProperty.Create(
            nameof(ChooseCommand), typeof(ICommand), typeof(ExternalLinkItemView));

        public string DisplayValue
        {
            get { return (string)GetValue(DisplayValueProperty); }
            set { SetValue(DisplayValueProperty, value); }
        }

        public SettingChooserItemView()
        {
            InitializeComponent();
        }

        public ICommand ChooseCommand
        {
            get => GetValue(ChooseCommandProperty) as ICommand;
            set => SetValue(ChooseCommandProperty, value);
        }
    }
}
