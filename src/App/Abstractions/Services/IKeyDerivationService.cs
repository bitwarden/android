namespace Bit.App.Abstractions
{
    public interface IKeyDerivationService
    {
        byte[] DeriveKey(string password, string salt);
    }
}