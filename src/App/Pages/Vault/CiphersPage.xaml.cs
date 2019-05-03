using System;

namespace Bit.App.Pages
{
    public partial class CiphersPage : BaseContentPage
    {
        private CiphersPageViewModel _vm;

        public CiphersPage()
        {
            InitializeComponent();
            SetActivityIndicator();
            _vm = BindingContext as CiphersPageViewModel;
            _vm.Page = this;
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, true, async () => {
                await _vm.LoadAsync();
            });
        }
    }
}
