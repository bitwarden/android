using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface IUserVerificationService
    {
        Task<bool> VerifyUser(string secret, VerificationType verificationType);
        Task<bool> VerifyMasterPasswordAsync(string masterPassword);
        Task<bool> HasMasterPasswordAsync(bool checkMasterKeyHash = false);
    }
}
