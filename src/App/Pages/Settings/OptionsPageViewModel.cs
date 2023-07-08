using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class OptionsPageViewModel : BaseViewModel
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;
        private readonly II18nService _i18nService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private bool _autofillSavePrompt;
        private string _autofillBlockedUris;
        private bool _favicon;
        private bool _autoTotpCopy;
        private int _clearClipboardSelectedIndex;
        private int _themeSelectedIndex;
        private int _autoDarkThemeSelectedIndex;
        private int _uriMatchSelectedIndex;
        private KeyValuePair<string, string> _selectedLocale;
        private bool _inited;
        private bool _updatingAutofill;
        private bool _showAndroidAutofillSettings;

        public OptionsPageViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _i18nService = ServiceContainer.Resolve<II18nService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();

            PageTitle = AppResources.Options;
            var iosIos = Device.RuntimePlatform == Device.iOS;

            ClearClipboardOptions = new List<KeyValuePair<int?, string>>
            {
                new KeyValuePair<int?, string>(null, AppResources.Never),
                new KeyValuePair<int?, string>(10, AppResources.TenSeconds),
                new KeyValuePair<int?, string>(20, AppResources.TwentySeconds),
                new KeyValuePair<int?, string>(30, AppResources.ThirtySeconds),
                new KeyValuePair<int?, string>(60, AppResources.OneMinute)
            };
            if (!iosIos)
            {
                ClearClipboardOptions.Add(new KeyValuePair<int?, string>(120, AppResources.TwoMinutes));
                ClearClipboardOptions.Add(new KeyValuePair<int?, string>(300, AppResources.FiveMinutes));
            }
            ThemeOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>(null, AppResources.ThemeDefault),
                new KeyValuePair<string, string>(ThemeManager.Light, AppResources.Light),
                new KeyValuePair<string, string>(ThemeManager.Dark, AppResources.Dark),
                new KeyValuePair<string, string>(ThemeManager.Black, AppResources.Black),
                new KeyValuePair<string, string>(ThemeManager.Nord, AppResources.Nord),
                new KeyValuePair<string, string>(ThemeManager.SolarizedDark, AppResources.SolarizedDark),
                new KeyValuePair<string, string>(ThemeManager.SolarizedDarkBlue, AppResources.SolarizedDarkBlue),
                new KeyValuePair<string, string>(ThemeManager.DarkPlus, AppResources.DarkPlus),
            };
            AutoDarkThemeOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>(ThemeManager.Dark, AppResources.Dark),
                new KeyValuePair<string, string>(ThemeManager.Black, AppResources.Black),
                new KeyValuePair<string, string>(ThemeManager.Nord, AppResources.Nord),
                new KeyValuePair<string, string>(ThemeManager.SolarizedDark, AppResources.SolarizedDark),
                new KeyValuePair<string, string>(ThemeManager.SolarizedDarkBlue, AppResources.SolarizedDarkBlue),
                new KeyValuePair<string, string>(ThemeManager.DarkPlus, AppResources.DarkPlus),
            };
            UriMatchOptions = new List<KeyValuePair<UriMatchType?, string>>
            {
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Domain, AppResources.BaseDomain),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Host, AppResources.Host),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.StartsWith, AppResources.StartsWith),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.RegularExpression, AppResources.RegEx),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Exact, AppResources.Exact),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Never, AppResources.Never),
            };
            LocalesOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>(null, AppResources.DefaultSystem)
            };
            LocalesOptions.AddRange(_i18nService.LocaleNames.ToList());
        }

        public List<KeyValuePair<int?, string>> ClearClipboardOptions { get; set; }
        public List<KeyValuePair<string, string>> ThemeOptions { get; set; }
        public List<KeyValuePair<string, string>> AutoDarkThemeOptions { get; set; }
        public List<KeyValuePair<UriMatchType?, string>> UriMatchOptions { get; set; }
        public List<KeyValuePair<string, string>> LocalesOptions { get; }

        public int ClearClipboardSelectedIndex
        {
            get => _clearClipboardSelectedIndex;
            set
            {
                if (SetProperty(ref _clearClipboardSelectedIndex, value))
                {
                    SaveClipboardChangedAsync().FireAndForget();
                }
            }
        }

        public int ThemeSelectedIndex
        {
            get => _themeSelectedIndex;
            set
            {
                if (SetProperty(ref _themeSelectedIndex, value,
                        additionalPropertyNames: new[] { nameof(ShowAutoDarkThemeOptions) })
                   )
                {
                    SaveThemeAsync().FireAndForget();
                }
            }
        }

        public bool ShowAutoDarkThemeOptions => ThemeOptions[ThemeSelectedIndex].Key == null;

        public int AutoDarkThemeSelectedIndex
        {
            get => _autoDarkThemeSelectedIndex;
            set
            {
                if (SetProperty(ref _autoDarkThemeSelectedIndex, value))
                {
                    SaveThemeAsync().FireAndForget();
                }
            }
        }

        public int UriMatchSelectedIndex
        {
            get => _uriMatchSelectedIndex;
            set
            {
                if (SetProperty(ref _uriMatchSelectedIndex, value))
                {
                    SaveDefaultUriAsync().FireAndForget();
                }
            }
        }

        public KeyValuePair<string, string> SelectedLocale
        {
            get => _selectedLocale;
            set
            {
                if (SetProperty(ref _selectedLocale, value))
                {
                    UpdateCurrentLocaleAsync().FireAndForget();
                }
            }
        }

        public bool Favicon
        {
            get => _favicon;
            set
            {
                if (SetProperty(ref _favicon, value))
                {
                    UpdateFaviconAsync().FireAndForget();
                }
            }
        }

        public bool AutoTotpCopy
        {
            get => _autoTotpCopy;
            set
            {
                if (SetProperty(ref _autoTotpCopy, value))
                {
                    UpdateAutoTotpCopyAsync().FireAndForget();
                }
            }
        }

        public bool AutofillSavePrompt
        {
            get => _autofillSavePrompt;
            set
            {
                if (SetProperty(ref _autofillSavePrompt, value))
                {
                    UpdateAutofillSavePromptAsync().FireAndForget();
                }
            }
        }

        public string AutofillBlockedUris
        {
            get => _autofillBlockedUris;
            set => SetProperty(ref _autofillBlockedUris, value);
        }

        public bool ShowAndroidAutofillSettings
        {
            get => _showAndroidAutofillSettings;
            set => SetProperty(ref _showAndroidAutofillSettings, value);
        }

        public async Task InitAsync()
        {
            AutofillSavePrompt = !(await _stateService.GetAutofillDisableSavePromptAsync()).GetValueOrDefault();

            var blockedUrisList = await _stateService.GetAutofillBlacklistedUrisAsync();
            AutofillBlockedUris = blockedUrisList != null ? string.Join(", ", blockedUrisList) : null;

            AutoTotpCopy = !(await _stateService.GetDisableAutoTotpCopyAsync() ?? false);

            Favicon = !(await _stateService.GetDisableFaviconAsync()).GetValueOrDefault();

            var theme = await _stateService.GetThemeAsync();
            ThemeSelectedIndex = ThemeOptions.FindIndex(k => k.Key == theme);

            var autoDarkTheme = await _stateService.GetAutoDarkThemeAsync() ?? "dark";
            AutoDarkThemeSelectedIndex = AutoDarkThemeOptions.FindIndex(k => k.Key == autoDarkTheme);

            var defaultUriMatch = await _stateService.GetDefaultUriMatchAsync();
            UriMatchSelectedIndex = defaultUriMatch == null ? 0 :
                UriMatchOptions.FindIndex(k => (int?)k.Key == defaultUriMatch);

            var clearClipboard = await _stateService.GetClearClipboardAsync();
            ClearClipboardSelectedIndex = ClearClipboardOptions.FindIndex(k => k.Key == clearClipboard);

            var appLocale = _stateService.GetLocale();
            SelectedLocale = appLocale == null ? LocalesOptions.First() : LocalesOptions.FirstOrDefault(kv => kv.Key == appLocale);

            _inited = true;
        }

        private async Task UpdateAutoTotpCopyAsync()
        {
            if (_inited)
            {
                // TODO: [PS-961] Fix negative function names
                await _stateService.SetDisableAutoTotpCopyAsync(!AutoTotpCopy);
            }
        }

        private async Task UpdateFaviconAsync()
        {
            if (_inited)
            {
                // TODO: [PS-961] Fix negative function names
                await _stateService.SetDisableFaviconAsync(!Favicon);
            }
        }

        private async Task SaveClipboardChangedAsync()
        {
            if (_inited && ClearClipboardSelectedIndex > -1)
            {
                await _stateService.SetClearClipboardAsync(ClearClipboardOptions[ClearClipboardSelectedIndex].Key);
            }
        }

        private async Task SaveThemeAsync()
        {
            if (_inited && ThemeSelectedIndex > -1)
            {
                await _stateService.SetThemeAsync(ThemeOptions[ThemeSelectedIndex].Key);
                await _stateService.SetAutoDarkThemeAsync(AutoDarkThemeOptions[AutoDarkThemeSelectedIndex].Key);
                ThemeManager.SetTheme(Application.Current.Resources);
                _messagingService.Send("updatedTheme");
            }
        }

        private async Task SaveDefaultUriAsync()
        {
            if (_inited && UriMatchSelectedIndex > -1)
            {
                await _stateService.SetDefaultUriMatchAsync((int?)UriMatchOptions[UriMatchSelectedIndex].Key);
            }
        }

        private async Task UpdateAutofillSavePromptAsync()
        {
            if (_inited)
            {
                // TODO: [PS-961] Fix negative function names
                await _stateService.SetAutofillDisableSavePromptAsync(!AutofillSavePrompt);
            }
        }

        public async Task UpdateAutofillBlockedUris()
        {
            if (_inited)
            {
                if (string.IsNullOrWhiteSpace(AutofillBlockedUris))
                {
                    await _stateService.SetAutofillBlacklistedUrisAsync(null);
                    AutofillBlockedUris = null;
                    return;
                }
                try
                {
                    var csv = AutofillBlockedUris;
                    var urisList = new List<string>();
                    foreach (var uri in csv.Split(','))
                    {
                        if (string.IsNullOrWhiteSpace(uri))
                        {
                            continue;
                        }
                        var cleanedUri = uri.Replace(System.Environment.NewLine, string.Empty).Trim();
                        if (!cleanedUri.StartsWith("http://") && !cleanedUri.StartsWith("https://") &&
                            !cleanedUri.StartsWith(Constants.AndroidAppProtocol))
                        {
                            continue;
                        }
                        urisList.Add(cleanedUri);
                    }
                    await _stateService.SetAutofillBlacklistedUrisAsync(urisList);
                    AutofillBlockedUris = string.Join(", ", urisList);
                }
                catch { }
            }
        }

        private async Task UpdateCurrentLocaleAsync()
        {
            if (!_inited)
            {
                return;
            }

            _stateService.SetLocale(SelectedLocale.Key);

            await _platformUtilsService.ShowDialogAsync(string.Format(AppResources.LanguageChangeXDescription, SelectedLocale.Value), AppResources.Language, AppResources.Ok);
        }
    }
}
