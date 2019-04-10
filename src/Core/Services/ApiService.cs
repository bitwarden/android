using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Newtonsoft.Json.Linq;
using System;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class ApiService
    {
        private readonly HttpClient _httpClient = new HttpClient();
        private readonly ITokenService _tokenService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private string _deviceType;
        private bool _usingBaseUrl = false;

        public ApiService(
            ITokenService tokenService,
            IPlatformUtilsService platformUtilsService)
        {
            _tokenService = tokenService;
            _platformUtilsService = platformUtilsService;
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

        public async Task<Tuple<IdentityTokenResponse, IdentityTwoFactorResponse>> PostIdentityTokenAsync()
        {
            var request = new HttpRequestMessage
            {
                RequestUri = new Uri(string.Concat(IdentityBaseUrl, "/connect/token")),
                Method = HttpMethod.Post
            };
            request.Headers.Add("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            request.Headers.Add("Accept", "application/json");
            request.Headers.Add("Device-Type", _deviceType);

            var response = await _httpClient.SendAsync(request);
            JObject responseJObject = null;
            if(response.Headers.Contains("content-type") &&
                response.Headers.GetValues("content-type").Any(h => h.Contains("application/json")))
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

        #endregion
    }
}
