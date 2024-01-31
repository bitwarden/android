using System;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPage : BaseContentPage
    {
        AutofillSettingsPageViewModel _vm;

        public AutofillSettingsPage()
        {
            InitializeComponent();
            _vm = BindingContext as AutofillSettingsPageViewModel;
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
