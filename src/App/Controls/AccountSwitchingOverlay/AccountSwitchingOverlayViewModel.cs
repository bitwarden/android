using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Microsoft.AppCenter.Crashes;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class AccountSwitchingOverlayViewModel : ExtendedViewModel
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;

        public AccountSwitchingOverlayViewModel(IStateService stateService,
            IMessagingService messagingService)
        {
            _stateService = stateService;
            _messagingService = messagingService;
            
            SelectAccountCommand = new AsyncCommand<AccountViewCellViewModel>(SelectAccountAsync,
#if !FDROID
                onException: ex => Crashes.TrackError(ex),
#endif
                allowsMultipleExecutions: false);
        }

        // this needs to be a new list every time for the binding to get updated,
        // XF doesn't currentlyl provide a direct way to update on same instance
        // https://github.com/xamarin/Xamarin.Forms/issues/1950
        public List<AccountView> AccountViews => _stateService?.AccountViews is null ? null : new List<AccountView>(_stateService.AccountViews);

        public bool AllowActiveAccountSelection { get; set; }

        public bool AllowAddAccountRow { get; set; }

        public ICommand SelectAccountCommand { get; }

        private async Task SelectAccountAsync(AccountViewCellViewModel item)
        {
            if (item.AccountView.IsAccount)
            {
                if (!item.AccountView.IsActive)
                {
                    await _stateService.SetActiveUserAsync(item.AccountView.UserId);
                    _messagingService.Send("switchedAccount");
                }
                else if (AllowActiveAccountSelection)
                {
                    _messagingService.Send("switchedAccount");
                }
            }
            else
            {
                _messagingService.Send("addAccount");
            }
        }

        public async Task RefreshAccountViewsAsync()
        {
            await _stateService.RefreshAccountViewsAsync(AllowAddAccountRow);

            Device.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(AccountViews)));
        }
    }
}
