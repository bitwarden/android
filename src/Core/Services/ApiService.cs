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
using System.Linq;
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
        private readonly HttpClient _httpClient;
        private readonly ITokenService _tokenService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly Func<bool, Task> _logoutCallbackAsync;
        private string _deviceType;

        public ApiService(
            ITokenService tokenService,
            IPlatformUtilsService platformUtilsService,
            Func<bool, Task> logoutCallbackAsync,
            HttpMessageHandler httpMessageHandler = null)
        {
            _tokenService = tokenService;
            _platformUtilsService = platformUtilsService;
            _logoutCallbackAsync = logoutCallbackAsync;
            var device = _platformUtilsService.GetDevice();
            _deviceType = device.ToString();
            if(httpMessageHandler != null)
            {
                _httpClient = new HttpClient(httpMessageHandler);
            }
            else
            {
                _httpClient = new HttpClient();
            }
        }

        public bool UrlsSet { get; private set; }
        public string ApiBaseUrl { get; set; }
        public string IdentityBaseUrl { get; set; }

        public void SetUrls(EnvironmentUrls urls)
        {
            UrlsSet = true;
            if(!string.IsNullOrWhiteSpace(urls.Base))
            {
                ApiBaseUrl = urls.Base + "/api";
                IdentityBaseUrl = urls.Base + "/identity";
                return;
            }
            if(!string.IsNullOrWhiteSpace(urls.Api) && !string.IsNullOrWhiteSpace(urls.Identity))
            {
                ApiBaseUrl = urls.Api;
                IdentityBaseUrl = urls.Identity;
                return;
            }
            // Local Dev
            //ApiBaseUrl = "http://localhost:4000";
            //IdentityBaseUrl = "http://localhost:33656";
            // Production
            ApiBaseUrl = "https://api.bitwarden.com";
            IdentityBaseUrl = "https://identity.bitwarden.com";
        }

        #region Auth APIs

        public async Task<Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>> PostIdentityTokenAsync(
            TokenRequest request)
        {
            var requestMessage = new HttpRequestMessage
            {
                RequestUri = new Uri(string.Concat(IdentityBaseUrl, "/connect/token")),
                Method = HttpMethod.Post,
                Content = new FormUrlEncodedContent(request.ToIdentityToken(_platformUtilsService.IdentityClientId))
            };
            requestMessage.Headers.Add("Accept", "application/json");
            requestMessage.Headers.Add("Device-Type", _deviceType);

            HttpResponseMessage response;
            try
            {
                response = await _httpClient.SendAsync(requestMessage);
            }
            catch
            {
                throw new ApiException(HandleWebError());
            }
            JObject responseJObject = null;
            if(IsJsonResponse(response))
            {
                var responseJsonString = await response.Content.ReadAsStringAsync();
                responseJObject = JObject.Parse(responseJsonString);
            }

            if(responseJObject != null)
            {
                if(response.IsSuccessStatusCode)
                {
                    return new Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>(
                        responseJObject.ToObject<IdentityTokenResponse>(), null);
                }
                else if(response.StatusCode == HttpStatusCode.BadRequest &&
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

        public Task PostRegisterAsync(RegisterRequest request)
        {
            return SendAsync<RegisterRequest, object>(HttpMethod.Post, "/accounts/register", request, false, false);
        }

        public Task PostAccountKeysAsync(KeysRequest request)
        {
            return SendAsync<KeysRequest, object>(HttpMethod.Post, "/accounts/keys", request, true, false);
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

        #endregion

        #region Attachments APIs

        public Task<CipherResponse> PostCipherAttachmentAsync(string id, MultipartFormDataContent data)
        {
            return SendAsync<MultipartFormDataContent, CipherResponse>(HttpMethod.Post,
                string.Concat("/ciphers/", id, "/attachment"), data, true, true);
        }

        public Task DeleteCipherAttachmentAsync(string id, string attachmentId)
        {
            return SendAsync<object, object>(HttpMethod.Delete,
                string.Concat("/ciphers/", id, "/attachment/", attachmentId), null, true, false);
        }

        public Task PostShareCipherAttachmentAsync(string id, string attachmentId, MultipartFormDataContent data,
            string organizationId)
        {
            return SendAsync<MultipartFormDataContent, object>(HttpMethod.Post,
                string.Concat("/ciphers/", id, "/attachment/", attachmentId, "/share?organizationId=", organizationId),
                data, true, false);
        }

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
            if(_tokenService.TokenNeedsRefresh())
            {
                var tokenResponse = await DoRefreshTokenAsync();
                accessToken = tokenResponse.AccessToken;
            }
            return accessToken;
        }

        public async Task<TResponse> SendAsync<TRequest, TResponse>(HttpMethod method, string path, TRequest body,
            bool authed, bool hasResponse)
        {
            using(var requestMessage = new HttpRequestMessage())
            {
                requestMessage.Method = method;
                requestMessage.RequestUri = new Uri(string.Concat(ApiBaseUrl, path));
                if(body != null)
                {
                    var bodyType = body.GetType();
                    if(bodyType == typeof(string))
                    {
                        requestMessage.Content = new StringContent((object)bodyType as string, Encoding.UTF8,
                            "application/x-www-form-urlencoded; charset=utf-8");
                    }
                    else if(bodyType == typeof(MultipartFormDataContent))
                    {
                        requestMessage.Content = body as MultipartFormDataContent;
                    }
                    else
                    {
                        requestMessage.Content = new StringContent(JsonConvert.SerializeObject(body, _jsonSettings),
                            Encoding.UTF8, "application/json");
                    }
                }

                requestMessage.Headers.Add("Device-Type", _deviceType);
                if(authed)
                {
                    var authHeader = await GetActiveBearerTokenAsync();
                    requestMessage.Headers.Add("Authorization", string.Concat("Bearer ", authHeader));
                }
                if(hasResponse)
                {
                    requestMessage.Headers.Add("Accept", "application/json");
                }

                HttpResponseMessage response;
                try
                {
                    response = await _httpClient.SendAsync(requestMessage);
                }
                catch
                {
                    throw new ApiException(HandleWebError());
                }
                if(hasResponse && response.IsSuccessStatusCode)
                {
                    var responseJsonString = await response.Content.ReadAsStringAsync();
                    return JsonConvert.DeserializeObject<TResponse>(responseJsonString);
                }
                else if(!response.IsSuccessStatusCode)
                {
                    var error = await HandleErrorAsync(response, false);
                    throw new ApiException(error);
                }
                return (TResponse)(object)null;
            }
        }

        public async Task<IdentityTokenResponse> DoRefreshTokenAsync()
        {
            var refreshToken = await _tokenService.GetRefreshTokenAsync();
            if(string.IsNullOrWhiteSpace(refreshToken))
            {
                throw new ApiException();
            }

            var decodedToken = _tokenService.DecodeToken();
            var requestMessage = new HttpRequestMessage
            {
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
            requestMessage.Headers.Add("Device-Type", _deviceType);

            HttpResponseMessage response;
            try
            {
                response = await _httpClient.SendAsync(requestMessage);
            }
            catch
            {
                throw new ApiException(HandleWebError());
            }
            if(response.IsSuccessStatusCode)
            {
                var responseJsonString = await response.Content.ReadAsStringAsync();
                var tokenResponse = JsonConvert.DeserializeObject<IdentityTokenResponse>(responseJsonString);
                await _tokenService.SetTokensAsync(tokenResponse.AccessToken, tokenResponse.RefreshToken);
                return tokenResponse;
            }
            else
            {
                var error = await HandleErrorAsync(response, true);
                throw new ApiException(error);
            }
        }

        private ErrorResponse HandleWebError()
        {
            return new ErrorResponse
            {
                StatusCode = HttpStatusCode.BadGateway,
                Message = "There is a problem connecting to the server."
            };
        }

        private async Task<ErrorResponse> HandleErrorAsync(HttpResponseMessage response, bool tokenError)
        {
            if((tokenError && response.StatusCode == HttpStatusCode.BadRequest) ||
                response.StatusCode == HttpStatusCode.Unauthorized || response.StatusCode == HttpStatusCode.Forbidden)
            {
                await _logoutCallbackAsync(true);
                return null;
            }
            JObject responseJObject = null;
            if(IsJsonResponse(response))
            {
                var responseJsonString = await response.Content.ReadAsStringAsync();
                responseJObject = JObject.Parse(responseJsonString);
            }
            return new ErrorResponse(responseJObject, response.StatusCode, tokenError);
        }

        private bool IsJsonResponse(HttpResponseMessage response)
        {
            return (response.Content?.Headers?.ContentType?.MediaType ?? string.Empty) == "application/json";
        }

        #endregion
    }
}
