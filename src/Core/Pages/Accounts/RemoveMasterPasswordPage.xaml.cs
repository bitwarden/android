using System;
using System.Collections.Generic;
using Bit.Core.Resources.Localization;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class RemoveMasterPasswordPage : BaseContentPage
    {
        private readonly RemoveMasterPasswordPageViewModel _vm;

        public Action NavigateAction { get; set; }

        public RemoveMasterPasswordPage()
        {
            InitializeComponent();
            _vm = BindingContext as RemoveMasterPasswordPageViewModel;

        }

        protected override async void OnAppearing()
        {
            await _vm.Init();
            _warningLabel.Text = string.Format(AppResources.RemoveMasterPasswordWarning,
                _vm.Organization.Name);
            _warningLabel2.Text = AppResources.RemoveMasterPasswordWarning2;
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

        protected override async void OnDisappearing()
        {
            NavigateAction?.Invoke();
        }
    }
}
