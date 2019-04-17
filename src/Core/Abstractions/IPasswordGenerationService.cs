using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IPasswordGenerationService
    {
        Task AddHistoryAsync(string password);
        Task ClearAsync();
        Task<string> GeneratePassphraseAsync(PasswordGenerationOptions options);
        Task<string> GeneratePasswordAsync(PasswordGenerationOptions options);
        Task<List<GeneratedPasswordHistory>> GetHistoryAsync();
        Task<PasswordGenerationOptions> GetOptionsAsync();
        Task<object> PasswordStrength(string password, List<string> userInputs = null);
        Task SaveOptionsAsync(PasswordGenerationOptions options);
    }
}