using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class OptionsPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStorageService _storageService;
        private readonly ITotpService _totpService;
        private readonly IStateService _stateService;

        private bool _disableFavicon;
        private bool _disableAutoTotpCopy;

        private int _clearClipboardSelectedIndex;
        private int _themeSelectedIndex;
        private int _uriMatchSelectedIndex;

        public OptionsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");

            PageTitle = AppResources.Options;

            ClearClipboardOptions = new List<KeyValuePair<int?, string>>
            {
                new KeyValuePair<int?, string>(null, AppResources.Never),
                new KeyValuePair<int?, string>(10, AppResources.TenSeconds),
                new KeyValuePair<int?, string>(20, AppResources.TwentySeconds),
                new KeyValuePair<int?, string>(30, AppResources.ThirtySeconds),
                new KeyValuePair<int?, string>(60, AppResources.OneMinute),
                new KeyValuePair<int?, string>(120, AppResources.TwoMinutes),
                new KeyValuePair<int?, string>(300, AppResources.FiveMinutes),
            };
            ThemeOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>(null, AppResources.Default),
                new KeyValuePair<string, string>("light", AppResources.Light),
                new KeyValuePair<string, string>("dark", AppResources.Dark),
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
                if(SetProperty(ref _clearClipboardSelectedIndex, value))
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
                if(SetProperty(ref _themeSelectedIndex, value))
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
                if(SetProperty(ref _uriMatchSelectedIndex, value))
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
                if(SetProperty(ref _disableFavicon, value))
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
                if(SetProperty(ref _disableAutoTotpCopy, value))
                {
                    var task = UpdateAutoTotpCopyAsync();
                }
            }
        }

        public async Task InitAsync()
        {
            DisableAutoTotpCopy = !(await _totpService.IsAutoCopyEnabledAsync());
            DisableFavicon = await _storageService.GetAsync<bool>(Constants.DisableFaviconKey);
            var theme = await _storageService.GetAsync<string>(Constants.ThemeKey);
            ThemeSelectedIndex = ThemeOptions.FindIndex(k => k.Key == theme);
            var defaultUriMatch = await _storageService.GetAsync<int?>(Constants.DefaultUriMatch);
            UriMatchSelectedIndex = defaultUriMatch == null ? 0 :
                UriMatchOptions.FindIndex(k => (int?)k.Key == defaultUriMatch);
            var clearClipboard = await _storageService.GetAsync<int?>(Constants.ClearClipboardKey);
            ClearClipboardSelectedIndex = ClearClipboardOptions.FindIndex(k => k.Key == clearClipboard);
        }

        private async Task UpdateAutoTotpCopyAsync()
        {
            await _storageService.SaveAsync(Constants.DisableAutoTotpCopyKey, DisableAutoTotpCopy);
        }

        private async Task UpdateDisableFaviconAsync()
        {
            await _storageService.SaveAsync(Constants.DisableFaviconKey, DisableFavicon);
            await _stateService.SaveAsync(Constants.DisableFaviconKey, DisableFavicon);
        }

        private async Task SaveClipboardChangedAsync()
        {
            if(ClearClipboardSelectedIndex > -1)
            {
                await _storageService.SaveAsync(Constants.ClearClipboardKey,
                    ClearClipboardOptions[ClearClipboardSelectedIndex].Key);
            }
        }

        private async Task SaveThemeAsync()
        {
            if(ThemeSelectedIndex > -1)
            {
                await _storageService.SaveAsync(Constants.ThemeKey, ThemeOptions[ThemeSelectedIndex].Key);
                // TODO: change theme
            }
        }

        private async Task SaveDefaultUriAsync()
        {
            if(UriMatchSelectedIndex > -1)
            {
                await _storageService.SaveAsync(Constants.DefaultUriMatch, UriMatchOptions[UriMatchSelectedIndex].Key);
            }
        }
    }
}
