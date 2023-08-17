using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class AppearanceSettingsPageViewModel : BaseViewModel
    {
        private readonly IStateService _stateService;
        private readonly ILogger _logger;
        private readonly II18nService _i18nService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;

        private bool _inited;
        private bool _showWebsiteIcons;

        public AppearanceSettingsPageViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>();
            _logger = ServiceContainer.Resolve<ILogger>();
            _i18nService = ServiceContainer.Resolve<II18nService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();

            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();

            LanguagePickerViewModel = new PickerViewModel<string>(
                deviceActionService,
                _logger,
                OnLanguageChangingAsync,
                AppResources.Language,
                _ => _inited,
                ex => HandleException(ex));

            ThemePickerViewModel = new PickerViewModel<string>(
                deviceActionService,
                _logger,
                key => OnThemeChangingAsync(key, DefaultDarkThemePickerViewModel.SelectedKey),
                AppResources.Theme,
                _ => _inited,
                ex => HandleException(ex));
            ThemePickerViewModel.SetAfterSelectionChanged(_ =>
                MainThread.InvokeOnMainThreadAsync(() =>
                {
                    TriggerPropertyChanged(nameof(ShowDefaultDarkThemePicker));
                }));

            DefaultDarkThemePickerViewModel = new PickerViewModel<string>(
                deviceActionService,
                _logger,
                key => OnThemeChangingAsync(ThemePickerViewModel.SelectedKey, key),
                AppResources.DefaultDarkTheme,
                _ => _inited,
                ex => HandleException(ex));

            ToggleShowWebsiteIconsCommand = CreateDefaultAsyncCommnad(ToggleShowWebsiteIconsAsync, _ => _inited);
        }

        public PickerViewModel<string> LanguagePickerViewModel { get; }
        public PickerViewModel<string> ThemePickerViewModel { get; }
        public PickerViewModel<string> DefaultDarkThemePickerViewModel { get; }

        public bool ShowDefaultDarkThemePicker => ThemePickerViewModel.SelectedKey == string.Empty;

        public bool ShowWebsiteIcons
        {
            get => _showWebsiteIcons;
            set
            {
                if (SetProperty(ref _showWebsiteIcons, value))
                {
                    ((ICommand)ToggleShowWebsiteIconsCommand).Execute(null);
                }
            }
        }

        public bool IsShowWebsiteIconsEnabled => ToggleShowWebsiteIconsCommand.CanExecute(null);

        public AsyncCommand ToggleShowWebsiteIconsCommand { get; }

        public async Task InitAsync()
        {
            _showWebsiteIcons = !(await _stateService.GetDisableFaviconAsync() ?? false);
            MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(ShowWebsiteIcons)));

            InitLanguagePicker();
            await InitThemePickerAsync();
            await InitDefaultDarkThemePickerAsync();

            _inited = true;

            MainThread.BeginInvokeOnMainThread(() =>
            {
                ToggleShowWebsiteIconsCommand.RaiseCanExecuteChanged();
                LanguagePickerViewModel.SelectOptionCommand.RaiseCanExecuteChanged();
                ThemePickerViewModel.SelectOptionCommand.RaiseCanExecuteChanged();
                DefaultDarkThemePickerViewModel.SelectOptionCommand.RaiseCanExecuteChanged();
            });
        }

        private void InitLanguagePicker()
        {
            var options = new Dictionary<string, string>
            {
                [string.Empty] = AppResources.DefaultSystem
            };
            _i18nService.LocaleNames
                .ToList()
                .ForEach(pair => options[pair.Key] = pair.Value);

            var selectedKey = _stateService.GetLocale() ?? string.Empty;

            LanguagePickerViewModel.Init(options, selectedKey, string.Empty);
        }

        private async Task InitThemePickerAsync()
        {
            var options = new Dictionary<string, string>
            {
                [string.Empty] = AppResources.ThemeDefault,
                [ThemeManager.Light] = AppResources.Light,
                [ThemeManager.Dark] = AppResources.Dark,
                [ThemeManager.Black] = AppResources.Black,
                [ThemeManager.Nord] = AppResources.Nord,
                [ThemeManager.SolarizedDark] = AppResources.SolarizedDark
            };

            var selectedKey = await _stateService.GetThemeAsync() ?? string.Empty;

            ThemePickerViewModel.Init(options, selectedKey, string.Empty);

            TriggerPropertyChanged(nameof(ShowDefaultDarkThemePicker));
        }

        private async Task InitDefaultDarkThemePickerAsync()
        {
            var options = new Dictionary<string, string>
            {
                [ThemeManager.Dark] = AppResources.Dark,
                [ThemeManager.Black] = AppResources.Black,
                [ThemeManager.Nord] = AppResources.Nord,
                [ThemeManager.SolarizedDark] = AppResources.SolarizedDark
            };

            var selectedKey = await _stateService.GetAutoDarkThemeAsync() ?? ThemeManager.Dark;

            DefaultDarkThemePickerViewModel.Init(options, selectedKey, ThemeManager.Dark);
        }

        private async Task<bool> OnLanguageChangingAsync(string selectedLanguage)
        {
            _stateService.SetLocale(selectedLanguage == string.Empty ? (string)null : selectedLanguage);

            await _platformUtilsService.ShowDialogAsync(string.Format(AppResources.LanguageChangeXDescription, LanguagePickerViewModel.SelectedValue), AppResources.Language, AppResources.Ok);
            return true;
        }

        private async Task<bool> OnThemeChangingAsync(string selectedTheme, string selectedDefaultDarkTheme)
        {
            await _stateService.SetThemeAsync(selectedTheme == string.Empty ? (string)null : selectedTheme);
            await _stateService.SetAutoDarkThemeAsync(selectedDefaultDarkTheme == string.Empty ? (string)null : selectedDefaultDarkTheme);

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                ThemeManager.SetTheme(Application.Current.Resources);
                _messagingService.Send(ThemeManager.UPDATED_THEME_MESSAGE_KEY);
            });
            return true;
        }

        private async Task ToggleShowWebsiteIconsAsync()
        {
            // TODO: [PS-961] Fix negative function names
            await _stateService.SetDisableFaviconAsync(!ShowWebsiteIcons);
        }

        private void ToggleShowWebsiteIconsCommand_CanExecuteChanged(object sender, EventArgs e)
        {
            TriggerPropertyChanged(nameof(IsShowWebsiteIconsEnabled));
        }

        internal void SubscribeEvents()
        {
            ToggleShowWebsiteIconsCommand.CanExecuteChanged += ToggleShowWebsiteIconsCommand_CanExecuteChanged;
        }

        internal void UnsubscribeEvents()
        {
            ToggleShowWebsiteIconsCommand.CanExecuteChanged -= ToggleShowWebsiteIconsCommand_CanExecuteChanged;
        }
    }
}
