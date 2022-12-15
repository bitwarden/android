﻿using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Zxcvbn;

namespace Bit.Core.Abstractions
{
    public interface IPasswordGenerationService
    {
        Task AddHistoryAsync(string password, CancellationToken token = default(CancellationToken));
        Task ClearAsync(string userId = null);
        void ClearCache();
        Task<string> GeneratePassphraseAsync(PasswordGenerationOptions options);
        Task<string> GeneratePasswordAsync(PasswordGenerationOptions options);
        Task<List<GeneratedPasswordHistory>> GetHistoryAsync();
        Task<(PasswordGenerationOptions, PasswordGeneratorPolicyOptions)> GetOptionsAsync();
        Task<(PasswordGenerationOptions, PasswordGeneratorPolicyOptions)>
            EnforcePasswordGeneratorPoliciesOnOptionsAsync(PasswordGenerationOptions options);
        Result PasswordStrength(string password, List<string> userInputs = null);
        Result PasswordStrength(string password, string email);
        Task SaveOptionsAsync(PasswordGenerationOptions options);
        void NormalizeOptions(PasswordGenerationOptions options, PasswordGeneratorPolicyOptions enforcedPolicyOptions);
    }
}
