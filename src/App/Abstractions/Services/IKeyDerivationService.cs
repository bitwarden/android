namespace Bit.App.Abstractions
{
    public interface IKeyDerivationService
    {
        byte[] DeriveKey(byte[] password, byte[] salt, uint rounds);
    }
}