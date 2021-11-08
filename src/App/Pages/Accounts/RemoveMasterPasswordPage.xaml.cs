using System;
using System.Collections.Generic;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class RemoveMasterPasswordPage : BaseContentPage
    {
        private readonly RemoveMasterPasswordPageViewModel _vm;

        public RemoveMasterPasswordPage()
        {
            InitializeComponent();
            _vm = BindingContext as RemoveMasterPasswordPageViewModel;

        }

        protected override async void OnAppearing()
        {
            await _vm.Init();
        }

        private async void Continue_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.MigrateAccount();
                await Navigation.PopModalAsync();

            }
        }

        private async void LeaveOrg_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                var confirm = await DisplayAlert(AppResources.LeaveOrganization,
                    string.Format(AppResources.LeaveOrganizationName, _vm.Organization.Name),
                    AppResources.Yes, AppResources.No);
                if (confirm)
                {
                    await _vm.LeaveOrganization();
                    await Navigation.PopModalAsync();
                }
            }
        }

        //private async void Close_Clicked(object sender, System.EventArgs e)
        //{
        //    if (DoOnce())
        //    {
        //        await Navigation.PopModalAsync();
        //    }
        //}
    }
}
