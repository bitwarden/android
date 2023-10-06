using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IUserPinService
    {
        Task SetupPinAsync(string pin, bool requireMasterPasswordOnRestart);
    }
}
