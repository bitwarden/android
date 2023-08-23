using System.Threading.Tasks;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Foundation;
using LocalAuthentication;

namespace Bit.iOS.Core.Services
{
    public class BiometricService : BaseBiometricService
    {
        public BiometricService(IStateService stateService, ICryptoService cryptoService)
            : base(stateService, cryptoService)
        {
        }

        public override async Task<bool> SetupBiometricAsync(string bioIntegritySrcKey = null)
        {
            if (bioIntegritySrcKey == null)
            {
                bioIntegritySrcKey = Bit.Core.Constants.BiometricIntegritySourceKey;
            }
            var state = GetState();
            if (state != null)
            {
                await _stateService.SetSystemBiometricIntegrityState(bioIntegritySrcKey, ToBase64(state));
                await _stateService.SetAccountBiometricIntegrityValidAsync(bioIntegritySrcKey);
            }

            return true;
        }

        public override async Task<bool> IsSystemBiometricIntegrityValidAsync(string bioIntegritySrcKey = null)
        {
            var state = GetState();
            if (state == null)
            {
                // Fallback for devices unable to retrieve state
                return true;
            }
            
            if (bioIntegritySrcKey == null)
            {
                bioIntegritySrcKey = Bit.Core.Constants.BiometricIntegritySourceKey;
            }
            var savedState = await _stateService.GetSystemBiometricIntegrityState(bioIntegritySrcKey);
            if (savedState != null)
            {
                return FromBase64(savedState).Equals(state);
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
    }
}
