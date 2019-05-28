using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IAuthService
    {
        string Email { get; set; }
        string MasterPasswordHash { get; set; }
        TwoFactorProviderType? SelectedTwoFactorProviderType { get; set; }
        Dictionary<TwoFactorProviderType, TwoFactorProvider> TwoFactorProviders { get; set; }
        Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProvidersData { get; set; }

        TwoFactorProviderType? GetDefaultTwoFactorProvider(bool u2fSupported);
        List<TwoFactorProvider> GetSupportedTwoFactorProviders();
        Task<AuthResult> LogInAsync(string email, string masterPassword);
        Task<AuthResult> LogInCompleteAsync(string email, string masterPassword, TwoFactorProviderType twoFactorProvider, string twoFactorToken, bool? remember = null);
        Task<AuthResult> LogInTwoFactorAsync(TwoFactorProviderType twoFactorProvider, string twoFactorToken, bool? remember = null);
        void LogOut(Action callback);
        void Init();
    }
}