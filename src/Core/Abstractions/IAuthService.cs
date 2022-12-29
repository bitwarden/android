using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IAuthService
    {
        string Email { get; set; }
        string MasterPasswordHash { get; set; }
        string Code { get; set; }
        string CodeVerifier { get; set; }
        string SsoRedirectUrl { get; set; }
        TwoFactorProviderType? SelectedTwoFactorProviderType { get; set; }
        Dictionary<TwoFactorProviderType, TwoFactorProvider> TwoFactorProviders { get; set; }
        Dictionary<TwoFactorProviderType, Dictionary<string, object>> TwoFactorProvidersData { get; set; }

        TwoFactorProviderType? GetDefaultTwoFactorProvider(bool fido2Supported);
        bool AuthingWithSso();
        bool AuthingWithPassword();
        List<TwoFactorProvider> GetSupportedTwoFactorProviders();
        Task<AuthResult> LogInAsync(string email, string masterPassword, string captchaToken);
        Task<AuthResult> LogInSsoAsync(string code, string codeVerifier, string redirectUrl, string orgId);
        Task<AuthResult> LogInCompleteAsync(string email, string masterPassword, TwoFactorProviderType twoFactorProvider, string twoFactorToken, bool? remember = null);
        Task<AuthResult> LogInTwoFactorAsync(TwoFactorProviderType twoFactorProvider, string twoFactorToken, string captchaToken, bool? remember = null);
        Task<AuthResult> LogInPasswordlessAsync(string email, string accessCode, string authRequestId, byte[] decryptionKey, string userKeyCiphered, string localHashedPasswordCiphered);

        Task<List<PasswordlessLoginResponse>> GetPasswordlessLoginRequestsAsync();
        Task<PasswordlessLoginResponse> GetPasswordlessLoginRequestByIdAsync(string id);
        Task<PasswordlessLoginResponse> GetPasswordlessLoginResponseAsync(string id, string accessCode);
        Task<PasswordlessLoginResponse> PasswordlessLoginAsync(string id, string pubKey, bool requestApproved);
        Task<PasswordlessLoginResponse> PasswordlessCreateLoginRequestAsync(string email);

        void LogOut(Action callback);
        void Init();
    }
}
