using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPage : BaseContentPage
    {
        private readonly IMessagingService _messagingService;
        private readonly LoginPageViewModel _vm;

        public LoginPage(string email = null)
        {
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _messagingService.Send("showStatusBar", true);
            InitializeComponent();
            _vm = BindingContext as LoginPageViewModel;
            _vm.Page = this;
            _vm.Email = email;
            MasterPasswordEntry = _masterPassword;
            if(Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }

            _email.ReturnType = ReturnType.Next;
            _email.ReturnCommand = new Command(() => _masterPassword.Focus());
        }

        public Entry MasterPasswordEntry { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if(string.IsNullOrWhiteSpace(_vm.Email))
            {
                RequestFocus(_email);
            }
            else
            {
                RequestFocus(_masterPassword);
            }
        }

        private async void LogIn_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.LogInAsync();
            }
        }

        private void Hint_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new HintPage()));
            }
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                _messagingService.Send("showStatusBar", false);
                await Navigation.PopModalAsync();
            }
        }
    }
}
