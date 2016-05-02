using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Services
{
    public class AuthService : IAuthService
    {
        private const string TokenKey = "token";

        private readonly ISecureStorageService _secureStorage;
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;

        public AuthService(
            ISecureStorageService secureStorage, 
            ICryptoService cryptoService,
            IApiService apiService)
        {
            _secureStorage = secureStorage;
            _cryptoService = cryptoService;
            _apiService = apiService;
        }

        public string Token
        {
            get
            {
                var tokenBytes = _secureStorage.Retrieve(TokenKey);
                return Encoding.UTF8.GetString(tokenBytes, 0, tokenBytes.Length);
            }
            set
            {
                if(value != null)
                {
                    var tokenBytes = Encoding.UTF8.GetBytes(value);
                    _secureStorage.Store(TokenKey, tokenBytes);
                }
                else
                {
                    _secureStorage.Delete(TokenKey);
                }
            }
        }

        public bool IsAuthenticated
        {
            get
            {
                return _cryptoService.Key != null && Token != null;
            }
        }

        public async Task<ApiResult<TokenResponse>> TokenPostAsync(TokenRequest request)
        {
            var requestContent = JsonConvert.SerializeObject(request);
            var response = await _apiService.Client.PostAsync("/auth/token", new StringContent(requestContent, Encoding.UTF8, "application/json"));
            if(!response.IsSuccessStatusCode)
            {
                return await _apiService.HandleErrorAsync<TokenResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<TokenResponse>(responseContent);
            return ApiResult<TokenResponse>.Success(responseObj);
        }
    }
}
