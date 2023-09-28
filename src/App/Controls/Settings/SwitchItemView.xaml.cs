using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class SwitchItemView : BaseSettingItemView
    {
        public static readonly BindableProperty IsToggledProperty = BindableProperty.Create(
            nameof(IsToggled), typeof(bool), typeof(SwitchItemView), null, BindingMode.TwoWay);

        public static readonly BindableProperty SwitchAutomationIdProperty = BindableProperty.Create(
            nameof(SwitchAutomationId), typeof(string), typeof(SwitchItemView), null, BindingMode.OneWay);

        public static readonly BindableProperty ToggleSwitchCommandProperty = BindableProperty.Create(
            nameof(ToggleSwitchCommand), typeof(ICommand), typeof(ExternalLinkItemView));

        public SwitchItemView()
        {
            InitializeComponent();
        }

        public bool IsToggled
        {
            get { return (bool)GetValue(IsToggledProperty); }
            set { SetValue(IsToggledProperty, value); }
        }

        public string SwitchAutomationId
        {
            get { return (string)GetValue(SwitchAutomationIdProperty); }
            set { SetValue(SwitchAutomationIdProperty, value); }
        }

        public ICommand ToggleSwitchCommand
        {
            get => GetValue(ToggleSwitchCommandProperty) as ICommand;
            set => SetValue(ToggleSwitchCommandProperty, value);
        }

        void ContentView_Tapped(System.Object sender, System.EventArgs e)
        {
            _switch.IsToggled = !_switch.IsToggled;
        }
    }
}
