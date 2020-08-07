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

        public bool SetupBiometric()
        {
            var state = getState();
            return _storageService.SaveAsync("biometricState", state).IsCompletedSuccessfully;
        }

        public bool ValidateIntegrity()
        {
            var oldState = _storageService.GetAsync<NSData>("biometricState").Result;
            if (oldState == null)
            {
                SetupBiometric();

                return true;
            } else {
                var state = getState();

                return oldState == state;
            }
        }

        private NSData getState()
        {
            var context = new LAContext();
            context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthenticationWithBiometrics, out _);

            return context.EvaluatedPolicyDomainState;
        }
    }
}
