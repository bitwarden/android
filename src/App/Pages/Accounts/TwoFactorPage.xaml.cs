using System;
using Xamarin.Forms;

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

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
        }

        private void Continue_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {

            }
        }

        private void Methods_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {

            }
        }
    }
}
