using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Response;

namespace Bit.Core.Services
{
    public class AuditService : IAuditService
    {
        private const string PwnedPasswordsApi = "https://api.pwnedpasswords.com/range/";

        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IApiService _apiService;

        private HttpClient _httpClient = new HttpClient();

        public AuditService(
            ICryptoFunctionService cryptoFunctionService,
            IApiService apiService)
        {
            _cryptoFunctionService = cryptoFunctionService;
            _apiService = apiService;
        }

        public async Task<int> PasswordLeakedAsync(string password)
        {
            var hashBytes = await _cryptoFunctionService.HashAsync(password, Enums.CryptoHashAlgorithm.Sha1);
            var hash = BitConverter.ToString(hashBytes).Replace("-", string.Empty).ToUpperInvariant();
            var hashStart = hash.Substring(0, 5);
            var hashEnding = hash.Substring(5);
            var response = await _httpClient.GetAsync(string.Concat(PwnedPasswordsApi, hashStart));
            var leakedHashes = await response.Content.ReadAsStringAsync();
            var match = leakedHashes.Split(new[] { "\r\n", "\r", "\n" }, StringSplitOptions.None)
                .FirstOrDefault(v => v.Split(':')[0] == hashEnding);
            if (match != null && int.TryParse(match.Split(':')[1], out var matchCount))
            {
                return matchCount;
            }
            return 0;
        }

        public async Task<List<BreachAccountResponse>> BreachedAccountsAsync(string username)
        {
            try
            {
                return await _apiService.GetHibpBreachAsync(username);
            }
            catch (ApiException e)
            {
                if (e.Error != null && e.Error.StatusCode == System.Net.HttpStatusCode.NotFound)
                {
                    return new List<BreachAccountResponse>();
                }
                throw;
            }
        }
    }
}
