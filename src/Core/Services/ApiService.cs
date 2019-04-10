using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class ApiService
    {
        private readonly HttpClient _httpClient = new HttpClient();
        private readonly ITokenService _tokenService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly Func<bool, Task> _logoutCallbackAsync;
        private string _deviceType;
        private bool _usingBaseUrl = false;

        public ApiService(
            ITokenService tokenService,
            IPlatformUtilsService platformUtilsService,
            Func<bool, Task> logoutCallbackAsync)
        {
            _tokenService = tokenService;
            _platformUtilsService = platformUtilsService;
            _logoutCallbackAsync = logoutCallbackAsync;
        }

        public bool UrlsSet { get; private set; }
        public string ApiBaseUrl { get; set; }
        public string IdentityBaseUrl { get; set; }

        public void SetUrls(EnvironmentUrls urls)
        {
            UrlsSet = true;
            if(!string.IsNullOrWhiteSpace(urls.Base))
            {
                _usingBaseUrl = true;
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

            var response = await _httpClient.SendAsync(requestMessage);
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
            var requestMessage = new HttpRequestMessage
            {
                Method = method,
                RequestUri = new Uri(string.Concat(ApiBaseUrl, path)),
            };

            if(body != null)
            {
                var bodyType = body.GetType();
                if(bodyType == typeof(string))
                {
                    requestMessage.Content = new StringContent((object)bodyType as string, Encoding.UTF8,
                        "application/x-www-form-urlencoded; charset=utf-8");
                }
                else if(false)
                {
                    // TODO: form data content
                }
                else
                {
                    requestMessage.Content = new StringContent(JsonConvert.SerializeObject(body),
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

            var response = await _httpClient.SendAsync(requestMessage);
            if(hasResponse && response.IsSuccessStatusCode)
            {
                var responseJsonString = await response.Content.ReadAsStringAsync();
                return JsonConvert.DeserializeObject<TResponse>(responseJsonString);
            }
            else if(response.IsSuccessStatusCode)
            {
                var error = await HandleErrorAsync(response, false);
                throw new ApiException(error);
            }
            return (TResponse)(object)null;
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

            var response = await _httpClient.SendAsync(requestMessage);
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
            return response.Headers.Contains("content-type") &&
                response.Headers.GetValues("content-type").Any(h => h.Contains("application/json"));
        }

        #endregion
    }
}
