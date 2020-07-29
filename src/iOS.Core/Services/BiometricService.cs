using Bit.Core.Abstractions;

namespace Bit.iOS.Core.Services
{
    public class BiometricService : IBiometricService
    {
        public bool SetupBiometric()
        {
            return true;
        }

        public bool ValidateIntegrity()
        {
            return true;
        }
    }
}
