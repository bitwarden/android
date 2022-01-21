using System;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class HomeViewModel : BaseViewModel
    {
        private readonly IStateService _stateService;

        public HomeViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");

            PageTitle = AppResources.Bitwarden;
        }

        public ExtendedObservableCollection<AccountView> AccountViews
        {
            get => _stateService.AccountViews;
        }

        public Action StartLoginAction { get; set; }
        public Action StartRegisterAction { get; set; }
        public Action StartSsoLoginAction { get; set; }
        public Action StartEnvironmentAction { get; set; }
        public Action CloseAction { get; set; }
    }
}
