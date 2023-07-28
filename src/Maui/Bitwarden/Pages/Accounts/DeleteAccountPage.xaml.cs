using System;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages.Accounts
{
    public partial class DeleteAccountPage : BaseContentPage
    {
        DeleteAccountViewModel _vm;

        public DeleteAccountPage()
        {
            InitializeComponent();
            _vm = BindingContext as DeleteAccountViewModel;
            _vm.Page = this;
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private async void DeleteAccount_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.DeleteAccountAsync();
            }
        }
    }
}
