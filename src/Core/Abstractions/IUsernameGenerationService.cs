using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IUsernameGenerationService
    {
        Task<string> GenerateAsync(UsernameGenerationOptions options);
        void ClearCache();
        Task<UsernameGenerationOptions> GetOptionsAsync();
        Task SaveOptionsAsync(UsernameGenerationOptions options);
    }
}
