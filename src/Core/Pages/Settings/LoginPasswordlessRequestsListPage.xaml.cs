using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.Response;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class LoginPasswordlessRequestsListPage : BaseContentPage
    {
        private LoginPasswordlessRequestsListViewModel _vm;

        public LoginPasswordlessRequestsListPage()
        {
            InitializeComponent();
            _vm = BindingContext as LoginPasswordlessRequestsListViewModel;
            _vm.Page = this;
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            try
            {
                _activityIndicatorGrid.IsVisible = true;

                await _vm.RefreshAsync();
                UpdatePlaceholder();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
            finally
            {
                _activityIndicatorGrid.IsVisible = false;
            }
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        public override async Task UpdateOnThemeChanged()
        {
            await base.UpdateOnThemeChanged();

            UpdatePlaceholder();
        }

        private void UpdatePlaceholder()
        {
#if ANDROID
            MainThread.BeginInvokeOnMainThread(() =>
                _emptyPlaceholder.Source = ImageSource.FromFile(ThemeManager.UsingLightTheme ? "empty_login_requests" : "empty_login_requests_dark"));
#endif
        }
    }
}

