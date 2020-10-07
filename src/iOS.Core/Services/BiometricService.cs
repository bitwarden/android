using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Foundation;
using LocalAuthentication;

namespace Bit.iOS.Core.Services
{
    public class BiometricService : IBiometricService
    {
        private IStorageService _storageService;

        public BiometricService(IStorageService storageService)
        {
            _storageService = storageService;
        }

        public async Task<bool> SetupBiometricAsync(string bioIntegrityKey = null)
        {
            if (bioIntegrityKey == null)
            {
                bioIntegrityKey = "biometricState";
            }
            var state = GetState();
            if (state != null)
            {
                await _storageService.SaveAsync(bioIntegrityKey, ToBase64(state));
            }

            return true;
        }

        public async Task<bool> ValidateIntegrityAsync(string bioIntegrityKey = null)
        {
            if (bioIntegrityKey == null)
            {
                bioIntegrityKey = "biometricState";
            }
            var oldState = await _storageService.GetAsync<string>(bioIntegrityKey);
            if (oldState == null)
            {
                // Fallback for upgraded devices
                await SetupBiometricAsync(bioIntegrityKey);

                return true;
            }
            else
            {
                var state = GetState();
                if (state != null)
                {
                    return FromBase64(oldState).Equals(state);
                }

                return true;
            }
        }

        private NSData GetState()
        {
            var context = new LAContext();
            context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthenticationWithBiometrics, out _);

            return context.EvaluatedPolicyDomainState;
        }

        private string ToBase64(NSData data)
        {
            return System.Convert.ToBase64String(data.ToArray());
        }

        private NSData FromBase64(string data)
        {
            var bytes = System.Convert.FromBase64String(data);
            return NSData.FromArray(bytes);
        }
    }
}
