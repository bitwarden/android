using System;
using System.Runtime.CompilerServices;

namespace Bit.App.Pages.Accounts
{
    public partial class VerificationCodePage : BaseContentPage
    {
        VerificationCodeViewModel _vm;

        public VerificationCodePage()
        {
            InitializeComponent();

            _vm = BindingContext as VerificationCodeViewModel;
            _vm.Page = this;
        }

        protected override void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (propertyName == nameof(VerificationCodeViewModel.ShowPassword))
            {
                RequestFocus(_secret);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            RequestFocus(_secret);
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void RequestOTP_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.RequestOTPAsync();
                _requestOTP.IsEnabled = false;
            }
        }
    }
}
