using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Serialization;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class ApiService : IApiService
    {
        private readonly JsonSerializerSettings _jsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver()
        };
        private readonly HttpClient _httpClient = new HttpClient();
        private readonly ITokenService _tokenService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly Func<bool, Task> _logoutCallbackAsync;

        public ApiService(
            ITokenService tokenService,
            IPlatformUtilsService platformUtilsService,
            Func<bool, Task> logoutCallbackAsync,
            string customUserAgent = null)
        {
            _tokenService = tokenService;
            _platformUtilsService = platformUtilsService;
            _logoutCallbackAsync = logoutCallbackAsync;
            var device = (int)_platformUtilsService.GetDevice();
            _httpClient.DefaultRequestHeaders.Add("Device-Type", device.ToString());
            if (!string.IsNullOrWhiteSpace(customUserAgent))
            {
                _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(customUserAgent);
            }
        }

        public bool UrlsSet { get; private set; }
        public string ApiBaseUrl { get; set; }
        public string IdentityBaseUrl { get; set; }
        public string EventsBaseUrl { get; set; }

        public void SetUrls(EnvironmentUrls urls)
        {
            UrlsSet = true;
            if (!string.IsNullOrWhiteSpace(urls.Base))
            {
                ApiBaseUrl = urls.Base + "/api";
                IdentityBaseUrl = urls.Base + "/identity";
                EventsBaseUrl = urls.Base + "/events";
                return;
            }

            ApiBaseUrl = urls.Api;
            IdentityBaseUrl = urls.Identity;
            EventsBaseUrl = urls.Events;

            // Production
            if (string.IsNullOrWhiteSpace(ApiBaseUrl))
            {
                ApiBaseUrl = "https://api.bitwarden.com";
            }
            if (string.IsNullOrWhiteSpace(IdentityBaseUrl))
            {
                IdentityBaseUrl = "https://identity.bitwarden.com";
            }
            if (string.IsNullOrWhiteSpace(EventsBaseUrl))
            {
                EventsBaseUrl = "https://events.bitwarden.com";
            }
        }

        #region Auth APIs

        public async Task<Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>> PostIdentityTokenAsync(
            TokenRequest request)
        {
            var requestMessage = new HttpRequestMessage
            {
                Version = new Version(1, 0),
                RequestUri = new Uri(string.Concat(IdentityBaseUrl, "/connect/token")),
                Method = HttpMethod.Post,
                Content = new FormUrlEncodedContent(request.ToIdentityToken(_platformUtilsService.IdentityClientId))
            };
            requestMessage.Headers.Add("Accept", "application/json");
            request.AlterIdentityTokenHeaders(requestMessage.Headers);

            HttpResponseMessage response;
            try
            {
                response = await _httpClient.SendAsync(requestMessage);
            }
            catch (Exception e)
            {
                throw new ApiException(HandleWebError(e));
            }
            JObject responseJObject = null;
            if (IsJsonResponse(response))
            {
                var responseJsonString = await response.Content.ReadAsStringAsync();
                responseJObject = JObject.Parse(responseJsonString);
            }

            if (responseJObject != null)
            {
                if (response.IsSuccessStatusCode)
                {
                    return new Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>(
                        responseJObject.ToObject<IdentityTokenResponse>(), null);
                }
                else if (response.StatusCode == HttpStatusCode.BadRequest &&
                    responseJObject.ContainsKey("TwoFactorProviders2") &&
                    responseJObject["TwoFactorProviders2"] != null &&
                    responseJObject["TwoFactorProviders2"].HasValues)
                {
                    return new Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>(
                        null, responseJObject.ToObject<IdentityTwoFactorResponse>());
                }
            }
            throw new ApiException(new ErrorResponse(responseJObject, response.StatusCode, true));
        }

        public async Task RefreshIdentityTokenAsync()
        {
            try
            {
                await DoRefreshTokenAsync();
            }
            catch
            {
                throw new ApiException();
            }
        }

        #endregion

        #region Account APIs

        public Task<ProfileResponse> GetProfileAsync()
        {
            return SendAsync<object, ProfileResponse>(HttpMethod.Get, "/accounts/profile", null, true, true);
        }

        public Task<PreloginResponse> PostPreloginAsync(PreloginRequest request)
        {
            return SendAsync<PreloginRequest, PreloginResponse>(HttpMethod.Post, "/accounts/prelogin",
                request, false, true);
        }

        public Task<long> GetAccountRevisionDateAsync()
        {
            return SendAsync<object, long>(HttpMethod.Get, "/accounts/revision-date", null, true, true);
        }

        public Task PostPasswordHintAsync(PasswordHintRequest request)
        {
            return SendAsync<PasswordHintRequest, object>(HttpMethod.Post, "/accounts/password-hint",
                request, false, false);
        }

        public Task SetPasswordAsync(SetPasswordRequest request)
        {
            return SendAsync<SetPasswordRequest, object>(HttpMethod.Post, "/accounts/set-password", request, true,
                false);
        }

        public Task PostRegisterAsync(RegisterRequest request)
        {
            return SendAsync<RegisterRequest, object>(HttpMethod.Post, "/accounts/register", request, false, false);
        }

        public Task PostAccountKeysAsync(KeysRequest request)
        {
            return SendAsync<KeysRequest, object>(HttpMethod.Post, "/accounts/keys", request, true, false);
        }

        public Task PostAccountVerifyPasswordAsync(PasswordVerificationRequest request)
        {
            return SendAsync<PasswordVerificationRequest, object>(HttpMethod.Post, "/accounts/verify-password", request,
                true, false);
        }

        #endregion

        #region Folder APIs

        public Task<FolderResponse> GetFolderAsync(string id)
        {
            return SendAsync<object, FolderResponse>(HttpMethod.Get, string.Concat("/folders/", id),
                null, true, true);
        }

        public Task<FolderResponse> PostFolderAsync(FolderRequest request)
        {
            return SendAsync<FolderRequest, FolderResponse>(HttpMethod.Post, "/folders", request, true, true);
        }

        public async Task<FolderResponse> PutFolderAsync(string id, FolderRequest request)
        {
            return await SendAsync<FolderRequest, FolderResponse>(HttpMethod.Put, string.Concat("/folders/", id),
                request, true, true);
        }

        public Task DeleteFolderAsync(string id)
        {
            return SendAsync<object, object>(HttpMethod.Delete, string.Concat("/folders/", id), null, true, false);
        }

        #endregion

        #region Send APIs

        public Task<SendResponse> GetSendAsync(string id) =>
            SendAsync<object, SendResponse>(HttpMethod.Get, $"/sends/{id}", null, true, true);

        public Task<SendResponse> PostSendAsync(SendRequest request) =>
            SendAsync<SendRequest, SendResponse>(HttpMethod.Post, "/sends", request, true, true);

        public Task<SendFileUploadDataResponse> PostFileTypeSendAsync(SendRequest request) =>
            SendAsync<SendRequest, SendFileUploadDataResponse>(HttpMethod.Post, "/sends/file/v2", request, true, true);

        public Task PostSendFileAsync(string sendId, string fileId, MultipartFormDataContent data) =>
            SendAsync<MultipartFormDataContent, object>(HttpMethod.Post, $"/sends/{sendId}/file/{fileId}", data, true, false);

        [Obsolete("Mar 25 2021: This method has been deprecated in favor of direct uploads. This method still exists for backward compatibility with old server versions.")]
        public Task<SendResponse> PostSendFileAsync(MultipartFormDataContent data) =>
            SendAsync<MultipartFormDataContent, SendResponse>(HttpMethod.Post, "/sends/file", data, true, true);

        public Task<SendFileUploadDataResponse> RenewFileUploadUrlAsync(string sendId, string fileId) =>
            SendAsync<object, SendFileUploadDataResponse>(HttpMethod.Get, $"/sends/{sendId}/file/{fileId}", null, true, true);

        public Task<SendResponse> PutSendAsync(string id, SendRequest request) =>
            SendAsync<SendRequest, SendResponse>(HttpMethod.Put, $"/sends/{id}", request, true, true);

        public Task<SendResponse> PutSendRemovePasswordAsync(string id) =>
            SendAsync<object, SendResponse>(HttpMethod.Put, $"/sends/{id}/remove-password", null, true, true);

        public Task DeleteSendAsync(string id) =>
            SendAsync<object, object>(HttpMethod.Delete, $"/sends/{id}", null, true, false);

        #endregion

        #region Cipher APIs

        public Task<CipherResponse> GetCipherAsync(string id)
        {
            return SendAsync<object, CipherResponse>(HttpMethod.Get, string.Concat("/ciphers/", id),
                null, true, true);
        }

        public Task<CipherResponse> PostCipherAsync(CipherRequest request)
        {
            return SendAsync<CipherRequest, CipherResponse>(HttpMethod.Post, "/ciphers", request, true, true);
        }

        public Task<CipherResponse> PostCipherCreateAsync(CipherCreateRequest request)
        {
            return SendAsync<CipherCreateRequest, CipherResponse>(HttpMethod.Post, "/ciphers/create",
                request, true, true);
        }

        public Task<CipherResponse> PutCipherAsync(string id, CipherRequest request)
        {
            return SendAsync<CipherRequest, CipherResponse>(HttpMethod.Put, string.Concat("/ciphers/", id),
                request, true, true);
        }

        public Task<CipherResponse> PutShareCipherAsync(string id, CipherShareRequest request)
        {
            return SendAsync<CipherShareRequest, CipherResponse>(HttpMethod.Put,
                string.Concat("/ciphers/", id, "/share"), request, true, true);
        }

        public Task PutCipherCollectionsAsync(string id, CipherCollectionsRequest request)
        {
            return SendAsync<CipherCollectionsRequest, object>(HttpMethod.Put,
                string.Concat("/ciphers/", id, "/collections"), request, true, false);
        }

        public Task DeleteCipherAsync(string id)
        {
            return SendAsync<object, object>(HttpMethod.Delete, string.Concat("/ciphers/", id), null, true, false);
        }

        public Task PutDeleteCipherAsync(string id)
        {
            return SendAsync<object, object>(HttpMethod.Put, string.Concat("/ciphers/", id, "/delete"), null, true, false);
        }

        public Task<CipherResponse> PutRestoreCipherAsync(string id)
        {
            return SendAsync<object, CipherResponse>(HttpMethod.Put, string.Concat("/ciphers/", id, "/restore"), null, true, true);
        }

        #endregion

        #region Attachments APIs

        [Obsolete("Mar 25 2021: This method has been deprecated in favor of direct uploads. This method still exists for backward compatibility with old server versions.")]
        public Task<CipherResponse> PostCipherAttachmentLegacyAsync(string id, MultipartFormDataContent data)
        {
            return SendAsync<MultipartFormDataContent, CipherResponse>(HttpMethod.Post,
                string.Concat("/ciphers/", id, "/attachment"), data, true, true);
        }

        public Task<AttachmentUploadDataResponse> PostCipherAttachmentAsync(string id, AttachmentRequest request)
        {
            return SendAsync<AttachmentRequest, AttachmentUploadDataResponse>(HttpMethod.Post,
                $"/ciphers/{id}/attachment/v2", request, true, true);
        }

        public Task<AttachmentResponse> GetAttachmentData(string cipherId, string attachmentId) =>
            SendAsync<AttachmentResponse>(HttpMethod.Get, $"/ciphers/{cipherId}/attachment/{attachmentId}", true);

        public Task DeleteCipherAttachmentAsync(string id, string attachmentId)
        {
            return SendAsync(HttpMethod.Delete,
                string.Concat("/ciphers/", id, "/attachment/", attachmentId), true);
        }

        public Task PostShareCipherAttachmentAsync(string id, string attachmentId, MultipartFormDataContent data,
            string organizationId)
        {
            return SendAsync<MultipartFormDataContent, object>(HttpMethod.Post,
                string.Concat("/ciphers/", id, "/attachment/", attachmentId, "/share?organizationId=", organizationId),
                data, true, false);
        }

        public Task<AttachmentUploadDataResponse> RenewAttachmentUploadUrlAsync(string cipherId, string attachmentId) =>
            SendAsync<AttachmentUploadDataResponse>(HttpMethod.Get, $"/ciphers/{cipherId}/attachment/{attachmentId}/renew", true);

        public Task PostAttachmentFileAsync(string cipherId, string attachmentId, MultipartFormDataContent data) =>
            SendAsync(HttpMethod.Post,
                $"/ciphers/{cipherId}/attachment/{attachmentId}", data, true);

        #endregion

        #region Sync APIs

        public Task<SyncResponse> GetSyncAsync()
        {
            return SendAsync<object, SyncResponse>(HttpMethod.Get, "/sync", null, true, true);
        }

        #endregion

        #region Two Factor APIs

        public Task PostTwoFactorEmailAsync(TwoFactorEmailRequest request)
        {
            return SendAsync<TwoFactorEmailRequest, object>(
                HttpMethod.Post, "/two-factor/send-email-login", request, false, false);
        }

        #endregion

        #region Device APIs

        public Task PutDeviceTokenAsync(string identifier, DeviceTokenRequest request)
        {
            return SendAsync<DeviceTokenRequest, object>(
                HttpMethod.Put, $"/devices/identifier/{identifier}/token", request, true, false);
        }

        #endregion

        #region Event APIs

        public async Task PostEventsCollectAsync(IEnumerable<EventRequest> request)
        {
            using (var requestMessage = new HttpRequestMessage())
            {
                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = HttpMethod.Post;
                requestMessage.RequestUri = new Uri(string.Concat(EventsBaseUrl, "/collect"));
                var authHeader = await GetActiveBearerTokenAsync();
                requestMessage.Headers.Add("Authorization", string.Concat("Bearer ", authHeader));
                requestMessage.Content = new StringContent(JsonConvert.SerializeObject(request, _jsonSettings),
                    Encoding.UTF8, "application/json");
                HttpResponseMessage response;
                try
                {
                    response = await _httpClient.SendAsync(requestMessage);
                }
                catch (Exception e)
                {
                    throw new ApiException(HandleWebError(e));
                }
                if (!response.IsSuccessStatusCode)
                {
                    var error = await HandleErrorAsync(response, false, false);
                    throw new ApiException(error);
                }
            }
        }

        #endregion

        #region HIBP APIs

        public Task<List<BreachAccountResponse>> GetHibpBreachAsync(string username)
        {
            return SendAsync<object, List<BreachAccountResponse>>(HttpMethod.Get,
                string.Concat("/hibp/breach?username=", username), null, true, true);
        }

        #endregion

        #region Helpers

        public async Task<string> GetActiveBearerTokenAsync()
        {
            var accessToken = await _tokenService.GetTokenAsync();
            if (_tokenService.TokenNeedsRefresh())
            {
                var tokenResponse = await DoRefreshTokenAsync();
                accessToken = tokenResponse.AccessToken;
            }
            return accessToken;
        }

        public async Task<object> PreValidateSso(string identifier)
        {
            var path = "/account/prevalidate?domainHint=" + WebUtility.UrlEncode(identifier);
            using (var requestMessage = new HttpRequestMessage())
            {
                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = HttpMethod.Get;
                requestMessage.RequestUri = new Uri(string.Concat(IdentityBaseUrl, path));
                requestMessage.Headers.Add("Accept", "application/json");
                
                HttpResponseMessage response;
                try
                {
                    response = await _httpClient.SendAsync(requestMessage);
                }
                catch (Exception e)
                {
                    throw new ApiException(HandleWebError(e));
                }
                if (!response.IsSuccessStatusCode)
                {
                    var error = await HandleErrorAsync(response, false, true);
                    throw new ApiException(error);
                }
                return null;
            }
        }

        public Task SendAsync(HttpMethod method, string path, bool authed) =>
            SendAsync<object, object>(method, path, null, authed, false);
        public Task SendAsync<TRequest>(HttpMethod method, string path, TRequest body, bool authed) =>
            SendAsync<TRequest, object>(method, path, body, authed, false);
        public Task<TResponse> SendAsync<TResponse>(HttpMethod method, string path, bool authed) =>
            SendAsync<object, TResponse>(method, path, null, authed, true);
        public async Task<TResponse> SendAsync<TRequest, TResponse>(HttpMethod method, string path, TRequest body,
            bool authed, bool hasResponse)
        {
            using (var requestMessage = new HttpRequestMessage())
            {
                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = method;
                requestMessage.RequestUri = new Uri(string.Concat(ApiBaseUrl, path));
                if (body != null)
                {
                    var bodyType = body.GetType();
                    if (bodyType == typeof(string))
                    {
                        requestMessage.Content = new StringContent((object)bodyType as string, Encoding.UTF8,
                            "application/x-www-form-urlencoded; charset=utf-8");
                    }
                    else if (bodyType == typeof(MultipartFormDataContent))
                    {
                        requestMessage.Content = body as MultipartFormDataContent;
                    }
                    else
                    {
                        requestMessage.Content = new StringContent(JsonConvert.SerializeObject(body, _jsonSettings),
                            Encoding.UTF8, "application/json");
                    }
                }

                if (authed)
                {
                    var authHeader = await GetActiveBearerTokenAsync();
                    requestMessage.Headers.Add("Authorization", string.Concat("Bearer ", authHeader));
                }
                if (hasResponse)
                {
                    requestMessage.Headers.Add("Accept", "application/json");
                }

                HttpResponseMessage response;
                try
                {
                    response = await _httpClient.SendAsync(requestMessage);
                }
                catch (Exception e)
                {
                    throw new ApiException(HandleWebError(e));
                }
                if (hasResponse && response.IsSuccessStatusCode)
                {
                    var responseJsonString = await response.Content.ReadAsStringAsync();
                    return JsonConvert.DeserializeObject<TResponse>(responseJsonString);
                }
                else if (!response.IsSuccessStatusCode)
                {
                    var error = await HandleErrorAsync(response, false, authed);
                    throw new ApiException(error);
                }
                return (TResponse)(object)null;
            }
        }

        public async Task<IdentityTokenResponse> DoRefreshTokenAsync()
        {
            var refreshToken = await _tokenService.GetRefreshTokenAsync();
            if (string.IsNullOrWhiteSpace(refreshToken))
            {
                throw new ApiException();
            }

            var decodedToken = _tokenService.DecodeToken();
            var requestMessage = new HttpRequestMessage
            {
                Version = new Version(1, 0),
                RequestUri = new Uri(string.Concat(IdentityBaseUrl, "/connect/token")),
                Method = HttpMethod.Post,
                Content = new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    ["grant_type"] = "refresh_token",
                    ["client_id"] = decodedToken.GetValue("client_id")?.Value<string>(),
                    ["refresh_token"] = refreshToken
                })
            };
            requestMessage.Headers.Add("Accept", "application/json");

            HttpResponseMessage response;
            try
            {
                response = await _httpClient.SendAsync(requestMessage);
            }
            catch (Exception e)
            {
                throw new ApiException(HandleWebError(e));
            }
            if (response.IsSuccessStatusCode)
            {
                var responseJsonString = await response.Content.ReadAsStringAsync();
                var tokenResponse = JsonConvert.DeserializeObject<IdentityTokenResponse>(responseJsonString);
                await _tokenService.SetTokensAsync(tokenResponse.AccessToken, tokenResponse.RefreshToken);
                return tokenResponse;
            }
            else
            {
                var error = await HandleErrorAsync(response, true, true);
                throw new ApiException(error);
            }
        }

        private ErrorResponse HandleWebError(Exception e)
        {
            return new ErrorResponse
            {
                StatusCode = HttpStatusCode.BadGateway,
                Message = "Exception message: " + e.Message
            };
        }

        private async Task<ErrorResponse> HandleErrorAsync(HttpResponseMessage response, bool tokenError, bool authed)
        {
            if (authed && ((tokenError && response.StatusCode == HttpStatusCode.BadRequest) ||
                response.StatusCode == HttpStatusCode.Unauthorized || response.StatusCode == HttpStatusCode.Forbidden))
            {
                await _logoutCallbackAsync(true);
                return null;
            }
            try
            {
                JObject responseJObject = null;
                if (IsJsonResponse(response))
                {
                    var responseJsonString = await response.Content.ReadAsStringAsync();
                    responseJObject = JObject.Parse(responseJsonString);
                }
                return new ErrorResponse(responseJObject, response.StatusCode, tokenError);
            }
            catch
            {
                return null;
            }
        }

        private bool IsJsonResponse(HttpResponseMessage response)
        {
            return (response.Content?.Headers?.ContentType?.MediaType ?? string.Empty) == "application/json";
        }

        #endregion
    }
}
