using System;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public partial class AppearanceSettingsPage : BaseContentPage
    {
        private readonly AppearanceSettingsPageViewModel _vm;

        public AppearanceSettingsPage()
        {
            InitializeComponent();
            _vm = BindingContext as AppearanceSettingsPageViewModel;
            _vm.Page = this;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            try
            {
                _vm.SubscribeEvents();
                await _vm.InitAsync();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                ServiceContainer.Resolve<IPlatformUtilsService>().ShowToast(null, null, AppResources.AnErrorHasOccurred);

                Navigation.PopModalAsync().FireAndForget();
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _vm.UnsubscribeEvents();
        }
    }
}
