using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls.PlatformConfiguration;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class OptionsPage : BaseContentPage
    {
        private readonly IAutofillHandler _autofillHandler;
        private readonly OptionsPageViewModel _vm;

        public OptionsPage()
        {
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            InitializeComponent();
            _vm = BindingContext as OptionsPageViewModel;
            _vm.Page = this;
            _themePicker.ItemDisplayBinding = new Binding("Value");
            _autoDarkThemePicker.ItemDisplayBinding = new Binding("Value");
            _uriMatchPicker.ItemDisplayBinding = new Binding("Value");
            _clearClipboardPicker.ItemDisplayBinding = new Binding("Value");
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
                _vm.ShowAndroidAutofillSettings = _autofillHandler.SupportsAutofillService();
            }
            else
            {
                _themePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _autoDarkThemePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _uriMatchPicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _clearClipboardPicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _languagePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
