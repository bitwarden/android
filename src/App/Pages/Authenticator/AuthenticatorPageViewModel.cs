using System;
using System.Threading.Tasks;
using System.Linq;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class AuthenticatorPageViewModel : BaseViewModel
    {
        #region Members

        private readonly IClipboardService _clipboardService;
        private readonly ITotpService _totpService;
        private readonly IUserService _userService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ICipherService _cipherService;

        private bool _showList = true;
        private bool _refreshing;
        private bool _loaded;
        private bool _websiteIconsEnabled = true;
        //private long _totpSec;
        #endregion

        #region Ctor

        public AuthenticatorPageViewModel()
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");

            PageTitle = AppResources.Authenticator;
            Items = new ExtendedObservableCollection<AuthenticatorPageListItem>();
        }

        #endregion

        #region Methods

        public async Task CopyAsync()
        {
            //await _clipboardService.CopyTextAsync(Password);
            //_platformUtilsService.ShowToast("success", null,
            //    string.Format(AppResources.ValueHasBeenCopied, AppResources.Password));
        }

        public async Task LoadAsync()
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            if (await _vaultTimeoutService.IsLockedAsync())
            {
                return;
            }

            try
            {
                await LoadDataAsync();
            }
            finally
            {
                ShowList = true;
                Refreshing = false;
            }
        }

        private async Task LoadDataAsync()
        {
            var _allCiphers = await _cipherService.GetAllDecryptedAsync();
            _allCiphers = _allCiphers.Where(c => c.Type == Core.Enums.CipherType.Login && c.Login.Totp != null).ToList();
            var filteredCiphers = _allCiphers.Select(c => new AuthenticatorPageListItem(c, WebsiteIconsEnabled)).ToList();
            Items.ResetWithRange(filteredCiphers);

            foreach (AuthenticatorPageListItem item in Items)
            {
                item.TotpUpdateCodeAsync();
            }

            //await TotpUpdateCodeAsync();
            //    var interval = _totpService.GetTimeInterval(Cipher.Login.Totp);
            //    await TotpTickAsync(interval);
            //    _totpInterval = DateTime.UtcNow;
            Device.StartTimer(new TimeSpan(0, 0, 1), () =>
            {
                foreach(AuthenticatorPageListItem item in Items)
                {
                    item.TotpTickAsync();
                }
                return true;
            });
            //}

            //private async Task TotpTickAsync(int intervalSeconds)
            //{
            //    var epoc = CoreHelpers.EpocUtcNow() / 1000;
            //    var mod = epoc % intervalSeconds;
            //    var totpSec = intervalSeconds - mod;
            //    TotpSec = totpSec.ToString();
            //    TotpLow = totpSec < 7;
            //    if (mod == 0)
            //    {
            //        await TotpUpdateCodeAsync();
            //    }
        }

        #endregion

        #region Properties

        public ExtendedObservableCollection<AuthenticatorPageListItem> Items { get; set; }
        public Command RefreshCommand { get; set; }

        public bool ShowList
        {
            get => _showList;
            set => SetProperty(ref _showList, value);
        }

        public bool Refreshing
        {
            get => _refreshing;
            set => SetProperty(ref _refreshing, value);
        }

        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public bool Loaded
        {
            get => _loaded;
            set => SetProperty(ref _loaded, value);
        }
        //public long TotpSec
        //{
        //    get => _totpSec;
        //    set => SetProperty(ref _totpSec, value);
        //}

        #endregion
    }
}
