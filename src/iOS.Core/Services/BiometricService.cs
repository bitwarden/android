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
                bioIntegrityKey = Bit.Core.Constants.BiometricIntegrityKey;
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
            var state = GetState();
            if (state == null)
            {
                // Fallback for devices unable to retrieve state
                return true;
            }
            
            if (bioIntegrityKey == null)
            {
                bioIntegrityKey = Bit.Core.Constants.BiometricIntegrityKey;
            }
            var oldState = await _storageService.GetAsync<string>(bioIntegrityKey);
            if (oldState == null)
            {
                oldState = await GetMigratedIntegrityState(bioIntegrityKey);
            }
            if (oldState != null)
            {
                return FromBase64(oldState).Equals(state);
            }
            return false;
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

        private async Task<string> GetMigratedIntegrityState(string bioIntegrityKey)
        {
            var legacyKey = "biometricState";
            if (bioIntegrityKey == Bit.Core.Constants.iOSAutoFillBiometricIntegrityKey)
            {
                legacyKey = "autofillBiometricState";
            }
            else if (bioIntegrityKey == Bit.Core.Constants.iOSExtensionBiometricIntegrityKey)
            {
                legacyKey = "extensionBiometricState";
            }
            
            // Original values are pulled from DB since the legacy keys were never defined in _preferenceStorageKeys
            var integrityState = await _storageService.GetAsync<string>(legacyKey);
            if (integrityState != null)
            {
                // Save original value to pref storage with new key
                await _storageService.SaveAsync(bioIntegrityKey, integrityState);
                
                // Remove value from DB storage with legacy key
                await _storageService.RemoveAsync(legacyKey);
                
                // Return value as if it was always in pref storage
                return integrityState;
            }
            
            // Return null since the state was never set
            return null;
        }
    }
}
