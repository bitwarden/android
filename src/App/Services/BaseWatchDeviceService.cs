using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models;
using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public abstract class BaseWatchDeviceService : IWatchDeviceService
    {
        private readonly ICipherService _cipherService;
        private readonly IEnvironmentService _environmentService;
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;

        protected BaseWatchDeviceService(ICipherService cipherService,
            IEnvironmentService environmentService,
            IStateService stateService,
            IVaultTimeoutService vaultTimeoutService)
        {
            _cipherService = cipherService;
            _environmentService = environmentService;
            _stateService = stateService;
            _vaultTimeoutService = vaultTimeoutService;
        }

        protected abstract bool CanSendData { get; }

        public async Task SyncDataToWatchAsync()
        {
            if (!CanSendData)
            {
                return;
            }

            var userData = await _stateService.GetActiveUserCustomDataAsync(a => a?.Profile is null ? null : new WatchDTO.UserDataDto
            {
                Id = a.Profile.UserId,
                Name = a.Profile.Name,
                Email = a.Profile.Email
            });
            var state = await GetStateAsync(userData?.Id);
            if (state != WatchState.Valid)
            {
                await SendDataToWatchAsync(new WatchDTO(state));
                return;
            }

            var ciphersWithTotp = await _cipherService.GetAllDecryptedAsync(c => c.DeletedDate == null && c.Login?.Totp != null);

            if (!ciphersWithTotp.Any())
            {
                await SendDataToWatchAsync(new WatchDTO(WatchState.Need2FAItem));
                return;
            }

            var watchDto = new WatchDTO(state)
            {
                Ciphers = ciphersWithTotp.Select(c => new SimpleCipherView(c)).ToList(),
                UserData = userData,
                EnvironmentData = new WatchDTO.EnvironmentUrlDataDto
                {
                    Base = _environmentService.BaseUrl,
                    Icons = _environmentService.IconsUrl
                }
                //SettingsData = new WatchDTO.SettingsDataDto
                //{
                //    VaultTimeoutInMinutes = await _vaultTimeoutService.GetVaultTimeout(userData?.Id),
                //    VaultTimeoutAction = await _stateService.GetVaultTimeoutActionAsync(userData?.Id) ?? VaultTimeoutAction.Lock
                //}
            };
            await SendDataToWatchAsync(watchDto);
        }

        private async Task<WatchState> GetStateAsync(string userId)
        {
            if (!await _stateService.IsAuthenticatedAsync() || userId is null)
            {
                return WatchState.NeedLogin;
            }

            //if (await _vaultTimeoutService.IsLockedAsync() ||
            //    await _vaultTimeoutService.ShouldLockAsync())
            //{
            //    return WatchState.NeedUnlock;
            //}

            if (!await _stateService.CanAccessPremiumAsync(userId))
            {
                return WatchState.NeedPremium;
            }

            return WatchState.Valid;
        }

        protected abstract Task SendDataToWatchAsync(WatchDTO watchDto);
    }
}
