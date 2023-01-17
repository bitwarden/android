using System;
using System.Collections.Generic;
using System.Linq;
using Bit.Core.Abstractions;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPasswordlessRequestsListPage : BaseContentPage
    {
        private LoginPasswordlessRequestsListViewModel _vm;

        public LoginPasswordlessRequestsListPage()
        {
            InitializeComponent();
            SetActivityIndicator(_mainContent);
            _vm = BindingContext as LoginPasswordlessRequestsListViewModel;
            _vm.Page = this;
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await LoadOnAppearedAsync(_mainLayout, false, _vm.RefreshAsync, _mainContent);
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

