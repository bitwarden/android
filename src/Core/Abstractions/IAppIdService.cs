using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IAppIdService
    {
        Task<string> GetAppIdAsync();
    }
}
