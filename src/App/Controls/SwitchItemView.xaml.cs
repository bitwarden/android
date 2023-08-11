using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class SwitchItemView : ContentView
    {
        public static readonly BindableProperty TitleProperty = BindableProperty.Create(
            nameof(Title), typeof(string), typeof(SwitchItemView), null, BindingMode.OneWay);

        public static readonly BindableProperty IsToggledProperty = BindableProperty.Create(
            nameof(IsToggled), typeof(bool), typeof(SwitchItemView), null, BindingMode.TwoWay);

        public static readonly BindableProperty SwitchAutomationIdProperty = BindableProperty.Create(
            nameof(SwitchAutomationId), typeof(string), typeof(SwitchItemView), null, BindingMode.OneWay);

        public SwitchItemView ()
        {
            InitializeComponent ();
        }

        public string Title
        {
            get { return (string)GetValue(TitleProperty); }
            set { SetValue(TitleProperty, value); }
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
    }
}
