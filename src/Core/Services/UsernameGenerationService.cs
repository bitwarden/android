using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class UsernameGenerationService : IUsernameGenerationService
    {
        private const string DEFAULT_GENERATED = "-";
        private const string CATCH_ALL_EMAIL_DOMAIN_FORMAT = "{0}@{1}";
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        private readonly UsernameGenerationOptions _defaultOptions = UsernameGenerationOptions.CreateDefault();
        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");
        private UsernameGenerationOptions _optionsCache;

        public UsernameGenerationService(
            ICryptoService cryptoService,
            IApiService apiService,
            IStateService stateService)
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
            _stateService = stateService;
        }

        public async Task<string> GenerateAsync(UsernameGenerationOptions options)
        {
            switch (options.Type)
            {
                case UsernameType.PlusAddressedEmail:
                    return await GeneratePlusAddressedEmailAsync(options);
                case UsernameType.CatchAllEmail:
                    return await GenerateCatchAllAsync(options);
                case UsernameType.ForwardedEmailAlias:
                    return await GenerateForwardedEmailAliasAsync(options);
                case UsernameType.RandomWord:
                    return await GenerateRandomWordAsync(options);
                default:
                    _logger.Value.Error("Error UsernameGenerationService: UsernameType not implemented.");
                    return string.Empty;
            }
        }

        public async Task<UsernameGenerationOptions> GetOptionsAsync()
        {
            if (_optionsCache == null)
            {
                var options = await _stateService.GetUsernameGenerationOptionsAsync();
                if (options == null)
                {
                    _optionsCache = _defaultOptions;
                }
                else
                {
                    options.Merge(_defaultOptions);
                    _optionsCache = options;
                }
            }

            return _optionsCache;
        }
        public async Task SaveOptionsAsync(UsernameGenerationOptions options)
        {
            await _stateService.SetUsernameGenerationOptionsAsync(options);
            _optionsCache = options;
        }

        public void ClearCache()
        {
            _optionsCache = null;
        }

        private async Task<string> GenerateRandomWordAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);

            var listLength = EEFLongWordList.Instance.List.Count - 1;
            var wordIndex = await _cryptoService.RandomNumberAsync(0, listLength);
            var randomWord = EEFLongWordList.Instance.List[wordIndex];

            if (string.IsNullOrWhiteSpace(randomWord))
            {
                _logger.Value.Error($"Error UsernameGenerationService: EEFLongWordList has NullOrWhiteSpace value at {wordIndex} index.");
                return DEFAULT_GENERATED;
            }

            if (options.CapitalizeRandomWordUsername)
            {
                randomWord = Capitalize(randomWord);
            }

            if (options.IncludeNumberRandomWordUsername)
            {
                randomWord = await AppendRandomNumberToRandomWordAsync(randomWord);
            }

            return randomWord;
        }

        private async Task<string> GeneratePlusAddressedEmailAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);

            if (string.IsNullOrWhiteSpace(options.PlusAddressedEmail) || options.PlusAddressedEmail.Length < 3)
            {
                return DEFAULT_GENERATED;
            }

            var atIndex = options.PlusAddressedEmail.IndexOf("@");
            if (atIndex < 1 || atIndex >= options.PlusAddressedEmail.Length - 1)
            {
                return options.PlusAddressedEmail;
            }

            if (options.PlusAddressedEmailType == UsernameEmailType.Random)
            {
                var randomString = await RandomStringAsync(8);
                return options.PlusAddressedEmail.Insert(atIndex, $"+{randomString}");
            }
            else
            {
                return options.PlusAddressedEmail.Insert(atIndex, $"+{options.EmailWebsite}");
            }
        }

        private async Task<string> GenerateCatchAllAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);
            var catchAllEmailDomain = options.CatchAllEmailDomain;
            string generatedString = string.Empty;

            if (string.IsNullOrWhiteSpace(catchAllEmailDomain))
            {
                return DEFAULT_GENERATED;
            }

            if (options.CatchAllEmailType == UsernameEmailType.Random)
            {
                var randomString = await RandomStringAsync(8);
                return string.Format(CATCH_ALL_EMAIL_DOMAIN_FORMAT, randomString, catchAllEmailDomain);
            }

            return string.Format(CATCH_ALL_EMAIL_DOMAIN_FORMAT, options.EmailWebsite, catchAllEmailDomain);
        }

        private async Task<string> GenerateForwardedEmailAliasAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);

            switch (options.ServiceType)
            {
                case ForwardedEmailServiceType.AnonAddy:
                    if (string.IsNullOrWhiteSpace(options.AnonAddyApiAccessToken) || string.IsNullOrWhiteSpace(options.AnonAddyDomainName))
                    {
                        return DEFAULT_GENERATED;
                    }
                    return await GetAnonAddyUsername(options.AnonAddyApiAccessToken, options.AnonAddyDomainName);

                case ForwardedEmailServiceType.FirefoxRelay:
                    if (string.IsNullOrWhiteSpace(options.FirefoxRelayApiAccessToken))
                    {
                        return DEFAULT_GENERATED;
                    }
                    return await GetFirefoxRelayUsername(options.FirefoxRelayApiAccessToken);

                case ForwardedEmailServiceType.SimpleLogin:
                    if (string.IsNullOrWhiteSpace(options.SimpleLoginApiKey))
                    {
                        return DEFAULT_GENERATED;
                    }
                    return await GetSimpleLoginUsername(options.SimpleLoginApiKey);
                default:
                    _logger.Value.Error("Error UsernameGenerationService: ForwardedEmailServiceType not implemented.");
                    return DEFAULT_GENERATED;
            }
        }

        private async Task<string> GetFirefoxRelayUsername(string apiToken)
        {
            var url = "https://relay.firefox.com/api/v1/relayaddresses/";

            return await _apiService.GetUsernameFromFirefoxRelay(url, apiToken);
        }

        private async Task<string> GetSimpleLoginUsername(string apiToken)
        {
            var url = "https://app.simplelogin.io/api/alias/random/new";

            return await _apiService.GetUsernameFromSimpleLogin(url, apiToken);
        }

        private async Task<string> GetAnonAddyUsername(string apiToken, string domain)
        {
            var url = "https://app.anonaddy.com/api/v1/aliases";

            return await _apiService.GetUsernameFromAnonAddy(url, apiToken, domain);
        }

        private async Task<string> RandomStringAsync(int length)
        {
            var str = "";
            var charSet = "abcdefghijklmnopqrstuvwxyz1234567890";

            for (var i = 0; i < length; i++)
            {
                var randomCharIndex = await _cryptoService.RandomNumberAsync(0, charSet.Length - 1);
                str += charSet[randomCharIndex];
            }

            return str;
        }

        private string Capitalize(string str)
        {
            return char.ToUpper(str[0]) + str.Substring(1);
        }

        private async Task<string> AppendRandomNumberToRandomWordAsync(string word)
        {
            if (string.IsNullOrWhiteSpace(word))
            {
                return word;
            }

            var randomNumber = await _cryptoService.RandomNumberAsync(1, 9999);

            return word + randomNumber.ToString("0000");
        }
    }
}
