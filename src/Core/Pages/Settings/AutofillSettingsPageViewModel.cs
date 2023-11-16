using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using CommunityToolkit.Mvvm.Input;

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
                () => _inited,
                ex => HandleException(ex));

            ToggleCopyTotpAutomaticallyCommand = CreateDefaultAsyncRelayCommand(ToggleCopyTotpAutomaticallyAsync, () => _inited, allowsMultipleExecutions: false);

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

        public AsyncRelayCommand ToggleCopyTotpAutomaticallyCommand { get; private set; }

        public async Task InitAsync()
        {
            await InitAndroidAutofillSettingsAsync();

            _copyTotpAutomatically = await _stateService.GetDisableAutoTotpCopyAsync() != true;

            await InitDefaultUriMatchDetectionPickerViewModelAsync();

            _inited = true;

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                TriggerPropertyChanged(nameof(CopyTotpAutomatically));

                ToggleUseAutofillServicesCommand.NotifyCanExecuteChanged();
                ToggleUseInlineAutofillCommand.NotifyCanExecuteChanged();
                ToggleUseAccessibilityCommand.NotifyCanExecuteChanged();
                ToggleUseDrawOverCommand.NotifyCanExecuteChanged();
                DefaultUriMatchDetectionPickerViewModel.SelectOptionCommand.NotifyCanExecuteChanged();
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
