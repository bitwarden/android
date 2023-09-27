using System.Threading.Tasks;
using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class UserPinService : IUserPinService
    {
        private readonly IStateService _stateService;
        private readonly ICryptoService _cryptoService;

        public UserPinService(IStateService stateService, ICryptoService cryptoService)
        {
            _stateService = stateService;
            _cryptoService = cryptoService;
        }

        public async Task SetupPinAsync(string pin, bool requireMasterPasswordOnRestart)
        {
            var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
            var email = await _stateService.GetEmailAsync();
            var pinKey = await _cryptoService.MakePinKeyAsync(pin, email, kdfConfig);
            var userKey = await _cryptoService.GetUserKeyAsync();
            var protectedPinKey = await _cryptoService.EncryptAsync(userKey.Key, pinKey);

            var encPin = await _cryptoService.EncryptAsync(pin);
            await _stateService.SetProtectedPinAsync(encPin.EncryptedString);

            if (requireMasterPasswordOnRestart)
            {
                await _stateService.SetPinKeyEncryptedUserKeyEphemeralAsync(protectedPinKey);
            }
            else
            {
                await _stateService.SetPinKeyEncryptedUserKeyAsync(protectedPinKey);
            }
        }
    }
}
