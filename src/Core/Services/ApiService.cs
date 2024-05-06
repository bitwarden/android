using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Serialization;
using DeviceType = Bit.Core.Enums.DeviceType;

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
        private readonly Func<Tuple<string, bool, bool>, Task> _logoutCallbackAsync;

        public ApiService(
            ITokenService tokenService,
            IPlatformUtilsService platformUtilsService,
            Func<Tuple<string, bool, bool>, Task> logoutCallbackAsync,
            string customUserAgent = null)
        {
            _tokenService = tokenService;
            _platformUtilsService = platformUtilsService;
            _logoutCallbackAsync = logoutCallbackAsync;
            var device = (int)_platformUtilsService.GetDevice();
            _httpClient.DefaultRequestHeaders.Add("Device-Type", device.ToString());
            _httpClient.DefaultRequestHeaders.Add("Bitwarden-Client-Name", _platformUtilsService.GetClientType().GetString());
            _httpClient.DefaultRequestHeaders.Add("Bitwarden-Client-Version", VersionHelpers.RemoveSuffix(_platformUtilsService.GetApplicationVersion()));
            if (!string.IsNullOrWhiteSpace(customUserAgent))
            {
                _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(customUserAgent);
            }
        }

        public bool UrlsSet { get; private set; }
        public string ApiBaseUrl { get; set; }
        public string IdentityBaseUrl { get; set; }
        public string EventsBaseUrl { get; set; }

        public void SetUrls(EnvironmentUrlData urls)
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

        public async Task<IdentityResponse> PostIdentityTokenAsync(TokenRequest request)
        {
            var requestMessage = new HttpRequestMessage
            {
                Version = new Version(1, 0),
                RequestUri = new Uri(string.Concat(IdentityBaseUrl, "/connect/token")),
                Method = HttpMethod.Post,
                Content = new FormUrlEncodedContent(request.ToIdentityToken(_platformUtilsService.GetClientType().GetString()))
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

            var identityResponse = new IdentityResponse(response.StatusCode, responseJObject);

            if (identityResponse.FailedToParse)
            {
                throw new ApiException(new ErrorResponse(responseJObject, response.StatusCode, true));
            }

            return identityResponse;
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
                request, false, true, sendToIdentity: true);
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
            return SendAsync<RegisterRequest, object>(HttpMethod.Post, "/accounts/register", request, false, false, sendToIdentity: true);
        }

        public Task PostAccountKeysAsync(KeysRequest request)
        {
            return SendAsync<KeysRequest, object>(HttpMethod.Post, "/accounts/keys", request, true, false);
        }

        public Task<VerifyMasterPasswordResponse> PostAccountVerifyPasswordAsync(PasswordVerificationRequest request)
        {
            return SendAsync<PasswordVerificationRequest, VerifyMasterPasswordResponse>(HttpMethod.Post, "/accounts/verify-password", request,
                true, true);
        }

        public Task PostAccountRequestOTP()
        {
            return SendAsync<object, object>(HttpMethod.Post, "/accounts/request-otp", null, true, false, null, false);
        }

        public Task PostAccountVerifyOTPAsync(VerifyOTPRequest request)
        {
            return SendAsync<VerifyOTPRequest, object>(HttpMethod.Post, "/accounts/verify-otp", request,
                true, false, null, false);
        }

        public Task PutUpdateTempPasswordAsync(UpdateTempPasswordRequest request)
        {
            return SendAsync<UpdateTempPasswordRequest, object>(HttpMethod.Put, "/accounts/update-temp-password",
                request, true, false);
        }

        public Task PostPasswordAsync(PasswordRequest request)
        {
            return SendAsync<PasswordRequest, object>(HttpMethod.Post, "/accounts/password", request, true, false);
        }

        public Task DeleteAccountAsync(DeleteAccountRequest request)
        {
            return SendAsync<DeleteAccountRequest, object>(HttpMethod.Delete, "/accounts", request, true, false);
        }

        public Task PostConvertToKeyConnectorAsync()
        {
            return SendAsync<object, object>(HttpMethod.Post, "/accounts/convert-to-key-connector", null, true, false);
        }

        public Task PostSetKeyConnectorKeyAsync(SetKeyConnectorKeyRequest request)
        {
            return SendAsync<SetKeyConnectorKeyRequest>(HttpMethod.Post, "/accounts/set-key-connector-key", request, true);
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

        public Task<bool> HasUnassignedCiphersAsync()
        {
            return SendAsync<object, bool>(HttpMethod.Get, "/ciphers/has-unassigned-ciphers", null, true, true);
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


        public Task<bool> GetKnownDeviceAsync(string email, string deviceIdentifier)
        {
            return SendAsync<object, bool>(HttpMethod.Get, "/devices/knowndevice", null, false, true, (message) =>
            {
                message.Headers.Add("X-Device-Identifier", deviceIdentifier);
                message.Headers.Add("X-Request-Email", CoreHelpers.Base64UrlEncode(Encoding.UTF8.GetBytes(email)));
            });
        }

        public Task PutDeviceTokenAsync(string identifier, DeviceTokenRequest request)
        {
            return SendAsync<DeviceTokenRequest, object>(
                HttpMethod.Put, $"/devices/identifier/{identifier}/token", request, true, false);
        }

        public Task<bool> GetDevicesExistenceByTypes(DeviceType[] deviceTypes)
        {
            return SendAsync<DeviceType[], bool>(
                HttpMethod.Post, "/devices/exist-by-types", deviceTypes, true, true);
        }

        public Task<DeviceResponse> GetDeviceByIdentifierAsync(string deviceIdentifier)
        {
            return SendAsync<object, DeviceResponse>(HttpMethod.Get, $"/devices/identifier/{deviceIdentifier}", null, true, true);
        }

        public Task<DeviceResponse> UpdateTrustedDeviceKeysAsync(string deviceIdentifier, TrustedDeviceKeysRequest trustedDeviceKeysRequest)
        {
            return SendAsync<TrustedDeviceKeysRequest, DeviceResponse>(HttpMethod.Put, $"/devices/{deviceIdentifier}/keys", trustedDeviceKeysRequest, true, true);
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

        #region Organizations APIs

        public Task<OrganizationKeysResponse> GetOrganizationKeysAsync(string id)
        {
            return SendAsync<object, OrganizationKeysResponse>(HttpMethod.Get, $"/organizations/{id}/keys", null, true, true);
        }

        public Task<OrganizationAutoEnrollStatusResponse> GetOrganizationAutoEnrollStatusAsync(string identifier)
        {
            return SendAsync<object, OrganizationAutoEnrollStatusResponse>(HttpMethod.Get,
                $"/organizations/{identifier}/auto-enroll-status", null, true, true);
        }

        public Task PostLeaveOrganizationAsync(string id)
        {
            return SendAsync<object, object>(HttpMethod.Post, $"/organizations/{id}/leave", null, true, false);
        }


        public Task<OrganizationDomainSsoDetailsResponse> GetOrgDomainSsoDetailsAsync(string userEmail)
        {
            return SendAsync<OrganizationDomainSsoDetailsRequest, OrganizationDomainSsoDetailsResponse>(HttpMethod.Post, $"/organizations/domain/sso/details", new OrganizationDomainSsoDetailsRequest { Email = userEmail }, false, true);
        }
        #endregion

        #region Organization User APIs

        public Task PutOrganizationUserResetPasswordEnrollmentAsync(string orgId, string userId,
            OrganizationUserResetPasswordEnrollmentRequest request)
        {
            return SendAsync<OrganizationUserResetPasswordEnrollmentRequest, object>(HttpMethod.Put,
                $"/organizations/{orgId}/users/{userId}/reset-password-enrollment", request, true, false);
        }

        #endregion

        #region Key Connector

        public async Task<KeyConnectorUserKeyResponse> GetMasterKeyFromKeyConnectorAsync(string keyConnectorUrl)
        {
            using (var requestMessage = new HttpRequestMessage())
            {
                var authHeader = await GetActiveBearerTokenAsync();

                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = HttpMethod.Get;
                requestMessage.RequestUri = new Uri(string.Concat(keyConnectorUrl, "/user-keys"));
                requestMessage.Headers.Add("Authorization", string.Concat("Bearer ", authHeader));

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
                var responseJsonString = await response.Content.ReadAsStringAsync();
                return JsonConvert.DeserializeObject<KeyConnectorUserKeyResponse>(responseJsonString);
            }
        }

        public async Task PostMasterKeyToKeyConnectorAsync(string keyConnectorUrl, KeyConnectorUserKeyRequest request)
        {
            using (var requestMessage = new HttpRequestMessage())
            {
                var authHeader = await GetActiveBearerTokenAsync();

                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = HttpMethod.Post;
                requestMessage.RequestUri = new Uri(string.Concat(keyConnectorUrl, "/user-keys"));
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
                    var error = await HandleErrorAsync(response, false, true);
                    throw new ApiException(error);
                }
            }
        }

        #endregion

        #region PasswordlessLogin

        public async Task<List<PasswordlessLoginResponse>> GetAuthRequestAsync()
        {
            var response = await SendAsync<object, PasswordlessLoginsResponse>(HttpMethod.Get, $"/auth-requests/", null, true, true);
            return response.Data;
        }

        public Task<PasswordlessLoginResponse> GetAuthRequestAsync(string id)
        {
            return SendAsync<object, PasswordlessLoginResponse>(HttpMethod.Get, $"/auth-requests/{id}", null, true, true);
        }

        public Task<PasswordlessLoginResponse> GetAuthResponseAsync(string id, string accessCode)
        {
            return SendAsync<object, PasswordlessLoginResponse>(HttpMethod.Get, $"/auth-requests/{id}/response?code={accessCode}", null, false, true);
        }

        public Task<PasswordlessLoginResponse> PostCreateRequestAsync(PasswordlessCreateLoginRequest passwordlessCreateLoginRequest, AuthRequestType authRequestType)
        {
            return SendAsync<object, PasswordlessLoginResponse>(HttpMethod.Post, authRequestType == AuthRequestType.AdminApproval ? "/auth-requests/admin-request" : "/auth-requests", passwordlessCreateLoginRequest, authRequestType == AuthRequestType.AdminApproval, true,
                (message) => message.Headers.Add("Device-Identifier", passwordlessCreateLoginRequest.DeviceIdentifier));
        }

        public Task<PasswordlessLoginResponse> PutAuthRequestAsync(string id, string encKey, string encMasterPasswordHash, string deviceIdentifier, bool requestApproved)
        {
            var request = new PasswordlessLoginRequest(encKey, encMasterPasswordHash, deviceIdentifier, requestApproved);
            return SendAsync<object, PasswordlessLoginResponse>(HttpMethod.Put, $"/auth-requests/{id}", request, true, true);
        }

        #endregion

        #region Configs

        public async Task<ConfigResponse> GetConfigsAsync()
        {
            var accessToken = await _tokenService.GetTokenAsync();
            return await SendAsync<object, ConfigResponse>(HttpMethod.Get, "/config/", null, !string.IsNullOrEmpty(accessToken), true);
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

        public async Task<SsoPrevalidateResponse> PreValidateSsoAsync(string identifier)
        {
            var path = "/sso/prevalidate?domainHint=" + WebUtility.UrlEncode(identifier);
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
                var responseJsonString = await response.Content.ReadAsStringAsync();
                return JsonConvert.DeserializeObject<SsoPrevalidateResponse>(responseJsonString);
            }
        }

        public Task SendAsync(HttpMethod method, string path, bool authed) =>
            SendAsync<object, object>(method, path, null, authed, false);
        public Task SendAsync<TRequest>(HttpMethod method, string path, TRequest body, bool authed) =>
            SendAsync<TRequest, object>(method, path, body, authed, false);
        public Task<TResponse> SendAsync<TResponse>(HttpMethod method, string path, bool authed) =>
            SendAsync<object, TResponse>(method, path, null, authed, true);
        public async Task<TResponse> SendAsync<TRequest, TResponse>(HttpMethod method, string path, TRequest body,
            bool authed, bool hasResponse, Action<HttpRequestMessage> alterRequest = null, bool logoutOnUnauthorized = true, bool sendToIdentity = false)
        {
            using (var requestMessage = new HttpRequestMessage())
            {
                var baseUrl = sendToIdentity ? IdentityBaseUrl : ApiBaseUrl;
                requestMessage.Version = new Version(1, 0);
                requestMessage.Method = method;

                if (!Uri.IsWellFormedUriString(baseUrl, UriKind.Absolute))
                {
                    throw new ApiException(new ErrorResponse
                    {
                        StatusCode = HttpStatusCode.BadGateway,
                        //Note: This message is hardcoded until AppResources.resx gets moved into Core.csproj
                        Message = "One or more URLs saved in the Settings are incorrect. Please revise it and try to log in again."
                    });
                }

                requestMessage.RequestUri = new Uri(string.Concat(baseUrl, path));

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
                alterRequest?.Invoke(requestMessage);

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
                    var error = await HandleErrorAsync(response, false, authed, logoutOnUnauthorized);
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

        public async Task<HttpResponseMessage> SendAsync(HttpRequestMessage requestMessage, CancellationToken cancellationToken = default)
        {
            HttpResponseMessage response;
            try
            {
                response = await _httpClient.SendAsync(requestMessage, cancellationToken);
            }
            catch (Exception e)
            {
                throw new ApiException(HandleWebError(e));
            }
            if (!response.IsSuccessStatusCode)
            {
                throw new ApiException(new ErrorResponse
                {
                    StatusCode = response.StatusCode,
                    Message = $"{requestMessage.RequestUri} error: {(int)response.StatusCode} {response.ReasonPhrase}."
                });
            }
            return response;
        }

        public async Task<string> GetFastmailAccountIdAsync(string apiKey)
        {
            using (var httpclient = new HttpClient())
            {
                HttpResponseMessage response;
                try
                {
                    httpclient.DefaultRequestHeaders.Add("Authorization", $"Bearer {apiKey}");
                    httpclient.DefaultRequestHeaders.Add("Accept", "application/json");
                    response = await httpclient.GetAsync(new Uri("https://api.fastmail.com/jmap/session"));
                }
                catch (Exception e)
                {
                    throw new ApiException(HandleWebError(e));
                }
                if (!response.IsSuccessStatusCode)
                {
                    throw new ApiException(new ErrorResponse
                    {
                        StatusCode = response.StatusCode,
                        Message = $"Fastmail error: {(int)response.StatusCode} {response.ReasonPhrase}."
                    });
                }
                var result = JObject.Parse(await response.Content.ReadAsStringAsync());
                return result["primaryAccounts"]?["https://www.fastmail.com/dev/maskedemail"]?.ToString();
            }
        }

        public async Task<List<Utilities.DigitalAssetLinks.Statement>> GetDigitalAssetLinksForRpAsync(string rpId)
        {
            using (var httpclient = new HttpClient())
            {
                HttpResponseMessage response;
                try
                {
                    httpclient.DefaultRequestHeaders.Add("Accept", "application/json");
                    response = await httpclient.GetAsync(new Uri($"https://{rpId}/.well-known/assetlinks.json"));
                }
                catch (Exception e)
                {
                    throw new ApiException(HandleWebError(e));
                }
                if (!response.IsSuccessStatusCode)
                {
                    throw new ApiException(new ErrorResponse
                    {
                        StatusCode = response.StatusCode,
                        Message = $"Digital Asset links Rp error: {(int)response.StatusCode} {response.ReasonPhrase}."
                    });
                }
                var json = await response.Content.ReadAsStringAsync();
                return JsonConvert.DeserializeObject<List<Utilities.DigitalAssetLinks.Statement>>(json);
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

        private async Task<ErrorResponse> HandleErrorAsync(HttpResponseMessage response, bool tokenError,
            bool authed, bool logoutOnUnauthorized = true)
        {
            if (authed
                &&
                (
                    (logoutOnUnauthorized && response.StatusCode == HttpStatusCode.Unauthorized)
                    ||
                    response.StatusCode == HttpStatusCode.Forbidden
                ))
            {
                await _logoutCallbackAsync(new Tuple<string, bool, bool>(null, false, true));
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

                if (authed && tokenError
                    &&
                    response.StatusCode == HttpStatusCode.BadRequest
                    &&
                    responseJObject?["error"]?.ToString() == "invalid_grant")
                {
                    await _logoutCallbackAsync(new Tuple<string, bool, bool>(null, false, true));
                    return null;
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
            if (response.Content?.Headers is null)
            {
                return false;
            }

            if (response.Content.Headers.ContentType?.MediaType == "application/json")
            {
                return true;
            }

            return response.Content.Headers.TryGetValues("Content-Type", out var vals)
                   &&
                   vals?.Any(v => v.Contains("application/json")) is true;
        }

        #endregion
    }
}
