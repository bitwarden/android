using System;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class PasswordHistoryPage : BaseContentPage
    {
        private PasswordHistoryPageViewModel _vm;

        public PasswordHistoryPage(string cipherId)
        {
            InitializeComponent();
            SetActivityIndicator();
            _vm = BindingContext as PasswordHistoryPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, true, async () =>
            {
                await _vm.InitAsync();
            });
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
