namespace Bit.Core.Abstractions
{
    public interface IBiometricService
    {
        bool SetupBiometric();
        bool ValidateIntegrity();
    }
}
