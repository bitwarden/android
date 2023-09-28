using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;

namespace Bit.App.Pages
{
    public partial class AutofillSettingsPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAutofillHandler _autofillHandler;
        private readonly IStateService _stateService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private bool _inited;
        private bool _copyTotpAutomatically;

        public AutofillSettingsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            _stateService = ServiceContainer.Resolve<IStateService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();

            DefaultUriMatchDetectionPickerViewModel = new PickerViewModel<UriMatchType>(
                _deviceActionService,
                ServiceContainer.Resolve<ILogger>(),
                DefaultUriMatchDetectionChangingAsync,
                AppResources.DefaultUriMatchDetection,
                _ => _inited,
                ex => HandleException(ex));

            ToggleCopyTotpAutomaticallyCommand = CreateDefaultAsyncCommnad(ToggleCopyTotpAutomaticallyAsync, _ => _inited);

            InitAndroidCommands();
            InitIOSCommands();
        }

        public bool CopyTotpAutomatically
        {
            get => _copyTotpAutomatically;
            set
            {
                if (SetProperty(ref _copyTotpAutomatically, value))
                {
                    ((ICommand)ToggleCopyTotpAutomaticallyCommand).Execute(null);
                }
            }
        }

        public PickerViewModel<UriMatchType> DefaultUriMatchDetectionPickerViewModel { get; }

        public AsyncCommand ToggleCopyTotpAutomaticallyCommand { get; private set; }

        public async Task InitAsync()
        {
            await InitAndroidAutofillSettingsAsync();

            _copyTotpAutomatically = await _stateService.GetDisableAutoTotpCopyAsync() != true;

            await InitDefaultUriMatchDetectionPickerViewModelAsync();

            _inited = true;

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                TriggerPropertyChanged(nameof(CopyTotpAutomatically));

                ToggleUseAutofillServicesCommand.RaiseCanExecuteChanged();
                ToggleUseInlineAutofillCommand.RaiseCanExecuteChanged();
                ToggleUseAccessibilityCommand.RaiseCanExecuteChanged();
                ToggleUseDrawOverCommand.RaiseCanExecuteChanged();
                DefaultUriMatchDetectionPickerViewModel.SelectOptionCommand.RaiseCanExecuteChanged();
            });
        }

        private async Task InitDefaultUriMatchDetectionPickerViewModelAsync()
        {
            var options = new Dictionary<UriMatchType, string>
            {
                [UriMatchType.Domain] = AppResources.BaseDomain,
                [UriMatchType.Host] = AppResources.Host,
                [UriMatchType.StartsWith] = AppResources.StartsWith,
                [UriMatchType.RegularExpression] = AppResources.RegEx,
                [UriMatchType.Exact] = AppResources.Exact,
                [UriMatchType.Never] = AppResources.Never
            };

            var defaultUriMatchDetection = ((UriMatchType?)await _stateService.GetDefaultUriMatchAsync()) ?? UriMatchType.Domain;

            DefaultUriMatchDetectionPickerViewModel.Init(options, defaultUriMatchDetection, UriMatchType.Domain);
        }

        private async Task ToggleCopyTotpAutomaticallyAsync()
        {
            await _stateService.SetDisableAutoTotpCopyAsync(!CopyTotpAutomatically);
        }

        private async Task<bool> DefaultUriMatchDetectionChangingAsync(UriMatchType type)
        {
            await _stateService.SetDefaultUriMatchAsync((int?)type);
            return true;
        }
    }
}
