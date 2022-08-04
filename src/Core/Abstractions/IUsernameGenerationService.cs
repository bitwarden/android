using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IUsernameGenerationService
    {
        Task<string> GenerateUsernameAsync(UsernameGenerationOptions options);
    }
}
