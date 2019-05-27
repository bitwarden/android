using System;

namespace Bit.App.Pages
{
    public partial class TwoFactorPage : BaseContentPage
    {
        private TwoFactorPageViewModel _vm;

        public TwoFactorPage()
        {
            InitializeComponent();
            _vm = BindingContext as TwoFactorPageViewModel;
            _vm.Page = this;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _vm.Init();
        }

        private async void Continue_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void Methods_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.AnotherMethodAsync();
            }
        }

        private async void ResendEmail_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SendEmailAsync(true, true);
            }
        }
    }
}
