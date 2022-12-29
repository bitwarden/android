using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IApiService
    {
        string ApiBaseUrl { get; set; }
        string IdentityBaseUrl { get; set; }
        string EventsBaseUrl { get; set; }
        bool UrlsSet { get; }

        Task DeleteCipherAsync(string id);
        Task DeleteCipherAttachmentAsync(string id, string attachmentId);
        Task DeleteFolderAsync(string id);
        Task<IdentityTokenResponse> DoRefreshTokenAsync();
        Task<long> GetAccountRevisionDateAsync();
        Task<string> GetActiveBearerTokenAsync();
        Task<CipherResponse> GetCipherAsync(string id);
        Task<FolderResponse> GetFolderAsync(string id);
        Task<ProfileResponse> GetProfileAsync();
        Task<SyncResponse> GetSyncAsync();
        Task PostAccountKeysAsync(KeysRequest request);
        Task PostAccountVerifyPasswordAsync(PasswordVerificationRequest request);
        Task PostAccountRequestOTP();
        Task PostAccountVerifyOTPAsync(VerifyOTPRequest request);
        Task<CipherResponse> PostCipherAsync(CipherRequest request);
        Task<CipherResponse> PostCipherCreateAsync(CipherCreateRequest request);
        Task<FolderResponse> PostFolderAsync(FolderRequest request);
        Task<IdentityResponse> PostIdentityTokenAsync(TokenRequest request);
        Task PostPasswordHintAsync(PasswordHintRequest request);
        Task SetPasswordAsync(SetPasswordRequest request);
        Task<PreloginResponse> PostPreloginAsync(PreloginRequest request);
        Task PostRegisterAsync(RegisterRequest request);
        Task<CipherResponse> PutCipherAsync(string id, CipherRequest request);
        Task PutCipherCollectionsAsync(string id, CipherCollectionsRequest request);
        Task<FolderResponse> PutFolderAsync(string id, FolderRequest request);
        Task<CipherResponse> PutShareCipherAsync(string id, CipherShareRequest request);
        Task PutDeleteCipherAsync(string id);
        Task<CipherResponse> PutRestoreCipherAsync(string id);
        Task RefreshIdentityTokenAsync();
        Task<SsoPrevalidateResponse> PreValidateSso(string identifier);
        Task<TResponse> SendAsync<TRequest, TResponse>(HttpMethod method, string path,
            TRequest body, bool authed, bool hasResponse, bool logoutOnUnauthorized = true);
        void SetUrls(EnvironmentUrls urls);
        [Obsolete("Mar 25 2021: This method has been deprecated in favor of direct uploads. This method still exists for backward compatibility with old server versions.")]
        Task<CipherResponse> PostCipherAttachmentLegacyAsync(string id, MultipartFormDataContent data);
        Task<AttachmentUploadDataResponse> PostCipherAttachmentAsync(string id, AttachmentRequest request);
        Task<AttachmentResponse> GetAttachmentData(string cipherId, string attachmentId);
        Task PostShareCipherAttachmentAsync(string id, string attachmentId, MultipartFormDataContent data,
            string organizationId);
        Task<AttachmentUploadDataResponse> RenewAttachmentUploadUrlAsync(string id, string attachmentId);
        Task PostAttachmentFileAsync(string id, string attachmentId, MultipartFormDataContent data);
        Task<List<BreachAccountResponse>> GetHibpBreachAsync(string username);
        Task PostTwoFactorEmailAsync(TwoFactorEmailRequest request);
        Task PutDeviceTokenAsync(string identifier, DeviceTokenRequest request);
        Task PostEventsCollectAsync(IEnumerable<EventRequest> request);
        Task PutUpdateTempPasswordAsync(UpdateTempPasswordRequest request);
        Task DeleteAccountAsync(DeleteAccountRequest request);
        Task<OrganizationKeysResponse> GetOrganizationKeysAsync(string id);
        Task<OrganizationAutoEnrollStatusResponse> GetOrganizationAutoEnrollStatusAsync(string identifier);
        Task PutOrganizationUserResetPasswordEnrollmentAsync(string orgId, string userId,
            OrganizationUserResetPasswordEnrollmentRequest request);
        Task<KeyConnectorUserKeyResponse> GetUserKeyFromKeyConnector(string keyConnectorUrl);
        Task PostUserKeyToKeyConnector(string keyConnectorUrl, KeyConnectorUserKeyRequest request);
        Task PostSetKeyConnectorKey(SetKeyConnectorKeyRequest request);
        Task PostConvertToKeyConnector();
        Task PostLeaveOrganization(string id);

        Task<SendResponse> GetSendAsync(string id);
        Task<SendResponse> PostSendAsync(SendRequest request);
        Task<SendFileUploadDataResponse> PostFileTypeSendAsync(SendRequest request);
        Task PostSendFileAsync(string sendId, string fileId, MultipartFormDataContent data);
        [Obsolete("Mar 25 2021: This method has been deprecated in favor of direct uploads. This method still exists for backward compatibility with old server versions.")]
        Task<SendResponse> PostSendFileAsync(MultipartFormDataContent data);
        Task<SendFileUploadDataResponse> RenewFileUploadUrlAsync(string sendId, string fileId);
        Task<SendResponse> PutSendAsync(string id, SendRequest request);
        Task<SendResponse> PutSendRemovePasswordAsync(string id);
        Task DeleteSendAsync(string id);
        Task<List<PasswordlessLoginResponse>> GetAuthRequestAsync();
        Task<PasswordlessLoginResponse> GetAuthRequestAsync(string id);
        Task<PasswordlessLoginResponse> GetAuthResponseAsync(string id, string accessCode);
        Task<PasswordlessLoginResponse> PutAuthRequestAsync(string id, string key, string masterPasswordHash, string deviceIdentifier, bool requestApproved);
        Task<PasswordlessLoginResponse> PostCreateRequestAsync(PasswordlessCreateLoginRequest passwordlessCreateLoginRequest);
        Task<string> GetUsernameFromAsync(ForwardedEmailServiceType service, UsernameGeneratorConfig config);
        Task<bool> GetKnownDeviceAsync(string email, string deviceIdentifier);
    }
}
