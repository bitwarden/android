using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface ITotpService
    {
        Task<string> GetCodeAsync(string key);
        int GetTimeInterval(string key);
        Task<bool> IsAutoCopyEnabledAsync();
    }
}
