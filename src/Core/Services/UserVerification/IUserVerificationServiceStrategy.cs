using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services.UserVerification
{
    public interface IUserVerificationServiceStrategy
    {
        Task<bool> VerifyUserForFido2Async(Fido2UserVerificationOptions options);
    }
}
