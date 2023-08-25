using System;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class SecuritySettingsPage : BaseContentPage
    {
        private SecuritySettingsPageViewModel _vm;

        public SecuritySettingsPage()
        {
            InitializeComponent();
            _vm = BindingContext as SecuritySettingsPageViewModel;
            _vm.Page = this;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            try
            {
                await _vm.InitAsync();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                ServiceContainer.Resolve<IPlatformUtilsService>().ShowToast(null, null, AppResources.AnErrorHasOccurred);

                Navigation.PopAsync().FireAndForget();
            }
        }
    }
}
