using System.Windows.Input;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class AccountSwitchingOverlayViewModel : ExtendedViewModel
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;

        public AccountSwitchingOverlayViewModel(IStateService stateService,
            IMessagingService messagingService,
            ILogger logger)
        {
            _stateService = stateService;
            _messagingService = messagingService;

            SelectAccountCommand = CreateDefaultAsyncRelayCommand<AccountViewCellViewModel>(SelectAccountAsync,
                onException: ex => logger.Exception(ex),
                allowsMultipleExecutions: false);

            LongPressAccountCommand = CreateDefaultAsyncRelayCommand<Tuple<ContentPage, AccountViewCellViewModel>>(LongPressAccountAsync,
                onException: ex => logger.Exception(ex),
                allowsMultipleExecutions: false);
        }

        // this needs to be a new list every time for the binding to get updated,
        // XF doesn't currentlyl provide a direct way to update on same instance
        // https://github.com/xamarin/Xamarin.Forms/issues/1950
        public List<AccountViewCellViewModel> AccountViews => _stateService?.AccountViews is null
            ? null
            : new List<AccountViewCellViewModel>(_stateService.AccountViews.Select(a => new AccountViewCellViewModel(a)).ToList());

        public bool AllowActiveAccountSelection { get; set; }

        public bool AllowAddAccountRow { get; set; }

        public ICommand SelectAccountCommand { get; }

        public ICommand LongPressAccountCommand { get; }

        public bool FromIOSExtension { get; set; }

        private async Task SelectAccountAsync(AccountViewCellViewModel item)
        {
            if (!item.AccountView.IsAccount)
            {
                _messagingService.Send(AccountsManagerMessageCommands.ADD_ACCOUNT);
                return;
            }

            if (!item.AccountView.IsActive)
            {
                await _stateService.SetActiveUserAsync(item.AccountView.UserId);
                _messagingService.Send(AccountsManagerMessageCommands.SWITCHED_ACCOUNT);
                if (FromIOSExtension)
                {
                    await _stateService.SaveExtensionActiveUserIdToStorageAsync(item.AccountView.UserId);
                }
            }
            else if (AllowActiveAccountSelection)
            {
                _messagingService.Send(AccountsManagerMessageCommands.SWITCHED_ACCOUNT);
            }
        }

        private async Task LongPressAccountAsync(Tuple<ContentPage, AccountViewCellViewModel> item)
        {
            var (page, account) = item;
            if (account.AccountView.IsAccount)
            {
                await AppHelpers.AccountListOptions(page, account);
            }
        }

        public async Task RefreshAccountViewsAsync()
        {
            await _stateService.RefreshAccountViewsAsync(AllowAddAccountRow);

            MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(AccountViews)));
        }
    }
}
