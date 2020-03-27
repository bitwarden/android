using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class OptionsPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStorageService _storageService;
        private readonly ITotpService _totpService;
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;


        private bool _autofillDisableSavePrompt;
        private string _autofillBlacklistedUris;
        private bool _disableFavicon;
        private bool _disableAutoTotpCopy;
        private int _clearClipboardSelectedIndex;
        private int _themeSelectedIndex;
        private int _uriMatchSelectedIndex;
        private bool _inited;
        private bool _updatingAutofill;
        private bool _showAndroidAutofillSettings;

        public OptionsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");

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
                new KeyValuePair<string, string>(null, AppResources.Default),
                new KeyValuePair<string, string>("light", AppResources.Light),
                new KeyValuePair<string, string>("dark", AppResources.Dark),
                new KeyValuePair<string, string>("black", AppResources.Black),
                new KeyValuePair<string, string>("nord", "Nord"),
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
        }

        public List<KeyValuePair<int?, string>> ClearClipboardOptions { get; set; }
        public List<KeyValuePair<string, string>> ThemeOptions { get; set; }
        public List<KeyValuePair<UriMatchType?, string>> UriMatchOptions { get; set; }

        public int ClearClipboardSelectedIndex
        {
            get => _clearClipboardSelectedIndex;
            set
            {
                if (SetProperty(ref _clearClipboardSelectedIndex, value))
                {
                    var task = SaveClipboardChangedAsync();
                }
            }
        }

        public int ThemeSelectedIndex
        {
            get => _themeSelectedIndex;
            set
            {
                if (SetProperty(ref _themeSelectedIndex, value))
                {
                    var task = SaveThemeAsync();
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
                    var task = SaveDefaultUriAsync();
                }
            }
        }

        public bool DisableFavicon
        {
            get => _disableFavicon;
            set
            {
                if (SetProperty(ref _disableFavicon, value))
                {
                    var task = UpdateDisableFaviconAsync();
                }
            }
        }

        public bool DisableAutoTotpCopy
        {
            get => _disableAutoTotpCopy;
            set
            {
                if (SetProperty(ref _disableAutoTotpCopy, value))
                {
                    var task = UpdateAutoTotpCopyAsync();
                }
            }
        }

        public bool AutofillDisableSavePrompt
        {
            get => _autofillDisableSavePrompt;
            set
            {
                if (SetProperty(ref _autofillDisableSavePrompt, value))
                {
                    var task = UpdateAutofillDisableSavePromptAsync();
                }
            }
        }

        public string AutofillBlacklistedUris
        {
            get => _autofillBlacklistedUris;
            set => SetProperty(ref _autofillBlacklistedUris, value);
        }

        public bool ShowAndroidAutofillSettings
        {
            get => _showAndroidAutofillSettings;
            set => SetProperty(ref _showAndroidAutofillSettings, value);
        }

        public async Task InitAsync()
        {
            AutofillDisableSavePrompt = (await _storageService.GetAsync<bool?>(
                Constants.AutofillDisableSavePromptKey)).GetValueOrDefault();
            var blacklistedUrisList = await _storageService.GetAsync<List<string>>(
                Constants.AutofillBlacklistedUrisKey);
            AutofillBlacklistedUris = blacklistedUrisList != null ? string.Join(", ", blacklistedUrisList) : null;
            DisableAutoTotpCopy = !(await _totpService.IsAutoCopyEnabledAsync());
            DisableFavicon = (await _storageService.GetAsync<bool?>(Constants.DisableFaviconKey)).GetValueOrDefault();
            var theme = await _storageService.GetAsync<string>(Constants.ThemeKey);
            ThemeSelectedIndex = ThemeOptions.FindIndex(k => k.Key == theme);
            var defaultUriMatch = await _storageService.GetAsync<int?>(Constants.DefaultUriMatch);
            UriMatchSelectedIndex = defaultUriMatch == null ? 0 :
                UriMatchOptions.FindIndex(k => (int?)k.Key == defaultUriMatch);
            var clearClipboard = await _storageService.GetAsync<int?>(Constants.ClearClipboardKey);
            ClearClipboardSelectedIndex = ClearClipboardOptions.FindIndex(k => k.Key == clearClipboard);
            _inited = true;
        }

        private async Task UpdateAutoTotpCopyAsync()
        {
            if (_inited)
            {
                await _storageService.SaveAsync(Constants.DisableAutoTotpCopyKey, DisableAutoTotpCopy);
            }
        }

        private async Task UpdateDisableFaviconAsync()
        {
            if (_inited)
            {
                await _storageService.SaveAsync(Constants.DisableFaviconKey, DisableFavicon);
                await _stateService.SaveAsync(Constants.DisableFaviconKey, DisableFavicon);
            }
        }

        private async Task SaveClipboardChangedAsync()
        {
            if (_inited && ClearClipboardSelectedIndex > -1)
            {
                await _storageService.SaveAsync(Constants.ClearClipboardKey,
                    ClearClipboardOptions[ClearClipboardSelectedIndex].Key);
            }
        }

        private async Task SaveThemeAsync()
        {
            if (_inited && ThemeSelectedIndex > -1)
            {
                var theme = ThemeOptions[ThemeSelectedIndex].Key;
                await _storageService.SaveAsync(Constants.ThemeKey, theme);
                if (Device.RuntimePlatform == Device.Android)
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.Restarting);
                    await Task.Delay(1000);
                }
                _messagingService.Send("updatedTheme", theme);
                if (Device.RuntimePlatform == Device.iOS)
                {
                    await Task.Delay(500);
                    await _platformUtilsService.ShowDialogAsync(AppResources.ThemeAppliedOnRestart);
                }
            }
        }

        private async Task SaveDefaultUriAsync()
        {
            if (_inited && UriMatchSelectedIndex > -1)
            {
                await _storageService.SaveAsync(Constants.DefaultUriMatch,
                    (int?)UriMatchOptions[UriMatchSelectedIndex].Key);
            }
        }

        private async Task UpdateAutofillDisableSavePromptAsync()
        {
            if (_inited)
            {
                await _storageService.SaveAsync(Constants.AutofillDisableSavePromptKey, AutofillDisableSavePrompt);
            }
        }

        public async Task UpdateAutofillBlacklistedUris()
        {
            if (_inited)
            {
                if (string.IsNullOrWhiteSpace(AutofillBlacklistedUris))
                {
                    await _storageService.RemoveAsync(Constants.AutofillBlacklistedUrisKey);
                    AutofillBlacklistedUris = null;
                    return;
                }
                try
                {
                    var csv = AutofillBlacklistedUris;
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
                    await _storageService.SaveAsync(Constants.AutofillBlacklistedUrisKey, urisList);
                    AutofillBlacklistedUris = string.Join(", ", urisList);
                }
                catch { }
            }
        }
    }
}
