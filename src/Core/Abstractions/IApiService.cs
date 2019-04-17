using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IApiService
    {
        string ApiBaseUrl { get; set; }
        string IdentityBaseUrl { get; set; }
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
        Task<CipherResponse> PostCipherAsync(CipherRequest request);
        Task<CipherResponse> PostCipherCreateAsync(CipherCreateRequest request);
        Task<FolderResponse> PostFolderAsync(FolderRequest request);
        Task<Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>> PostIdentityTokenAsync(TokenRequest request);
        Task PostPasswordHintAsync(PasswordHintRequest request);
        Task<PreloginResponse> PostPreloginAsync(PreloginRequest request);
        Task PostRegisterAsync(RegisterRequest request);
        Task<CipherResponse> PutCipherAsync(string id, CipherRequest request);
        Task PutCipherCollectionsAsync(string id, CipherCollectionsRequest request);
        Task<FolderResponse> PutFolderAsync(string id, FolderRequest request);
        Task<CipherResponse> PutShareCipherAsync(string id, CipherShareRequest request);
        Task RefreshIdentityTokenAsync();
        Task<TResponse> SendAsync<TRequest, TResponse>(HttpMethod method, string path,
            TRequest body, bool authed, bool hasResponse);
        void SetUrls(EnvironmentUrls urls);
        Task<CipherResponse> PostCipherAttachmentAsync(string id, MultipartFormDataContent data);
        Task PostShareCipherAttachmentAsync(string id, string attachmentId, MultipartFormDataContent data,
            string organizationId);
        Task<List<BreachAccountResponse>> GetHibpBreachAsync(string username);
    }
}
