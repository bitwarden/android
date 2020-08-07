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

        public async Task<bool> SetupBiometric()
        {
            var state = GetState();
            await _storageService.SaveAsync("biometricState", state);

            return true;
        }

        public async Task<bool> ValidateIntegrity()
        {
            var oldState = await _storageService.GetAsync<NSData>("biometricState");
            if (oldState == null)
            {
                // Fallback for upgraded devices
                await SetupBiometric();

                return true;
            }
            else
            {
                var state = GetState();

                return oldState == state;
            }
        }

        private NSData GetState()
        {
            var context = new LAContext();
            context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthenticationWithBiometrics, out _);

            return context.EvaluatedPolicyDomainState;
        }
    }
}
