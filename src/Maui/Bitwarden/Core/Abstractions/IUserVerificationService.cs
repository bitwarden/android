using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface IUserVerificationService
    {
        Task<bool> VerifyUser(string secret, VerificationType verificationType);
    }
}
