using System;

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
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, true, async () => {
                await _vm.InitAsync();
            });
        }
    }
}
