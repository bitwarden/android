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

        public async Task<bool> SetupBiometricAsync()
        {
            var state = GetState();
            await _storageService.SaveAsync("biometricState", ToBase64(state));

            return true;
        }

        public async Task<bool> ValidateIntegrityAsync()
        {
            var oldState = await _storageService.GetAsync<string>("biometricState");
            if (oldState == null)
            {
                // Fallback for upgraded devices
                await SetupBiometricAsync();

                return true;
            }
            else
            {
                var state = GetState();

                return FromBase64(oldState) == state;
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
