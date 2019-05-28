using Bit.App.Controls;
using System;
using System.Threading.Tasks;

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
            DuoWebView = _duoWebView;
            SetActivityIndicator();
        }

        public HybridWebView DuoWebView { get; set; }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_scrollView, true, () =>
            {
                _vm.Init();
                return Task.FromResult(0);
            });
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
