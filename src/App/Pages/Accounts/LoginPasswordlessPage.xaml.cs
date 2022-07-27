namespace Bit.App.Pages
{
    public partial class LoginPasswordlessPage : BaseContentPage
    {
        private LoginPasswordlessViewModel _vm;

        public LoginPasswordlessPage(string email, string deviceType, string ipAddress, string location, string fingerprintPhrase)
        {
            InitializeComponent();
            _vm = BindingContext as LoginPasswordlessViewModel;
            _vm.Page = this;
            _vm.Email = email;
            _vm.DeviceType = deviceType;
            _vm.IpAddress = ipAddress;
            _vm.NearLocation = location;
            _vm.FingerprintPhrase = fingerprintPhrase;
        }

        protected override void OnAppearing()
        {
            _vm.InitAsync();
            base.OnAppearing();
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
