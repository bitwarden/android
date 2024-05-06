using Microsoft.Maui.Controls.PlatformConfiguration;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class SharePage : BaseContentPage
    {
        private SharePageViewModel _vm;

        public SharePage(string cipherId)
        {
            InitializeComponent();
            _vm = BindingContext as SharePageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            SetActivityIndicator();
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
            else
            {
                _organizationPicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            }
            _organizationPicker.ItemDisplayBinding = new Binding("Key");
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, () => _vm.LoadAsync());
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
