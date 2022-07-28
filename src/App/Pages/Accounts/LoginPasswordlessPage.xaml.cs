using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPasswordlessPage : BaseContentPage
    {
        private LoginPasswordlessViewModel _vm;

        public LoginPasswordlessPage(string fingerprintPhrase, string email, string deviceType, string ipAddress, string location, string origin, DateTime requestDate)
        {
            InitializeComponent();
            _vm = BindingContext as LoginPasswordlessViewModel;
            _vm.Page = this;
            _vm.Origin = origin;
            _vm.Email = email;
            _vm.DeviceType = deviceType;
            _vm.IpAddress = ipAddress;
            _vm.NearLocation = location;
            _vm.FingerprintPhrase = fingerprintPhrase;
            _vm.RequestDate = requestDate;
            if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_closeItem);
            }
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
