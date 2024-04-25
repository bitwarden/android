namespace Bit.Core.Services
{
    public interface IAssetLinksService
    {
        Task<bool> ValidateAssetLinksAsync(string rpId, string packageName, string normalizedFingerprint);
    }
}
