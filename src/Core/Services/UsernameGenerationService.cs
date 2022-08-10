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
        private const string DefaultGenerated = "-";
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        private UsernameGenerationOptions _defaultOptions = new UsernameGenerationOptions(true);
        private UsernameGenerationOptions _optionsCache;

        public UsernameGenerationService(
            ICryptoService cryptoService,
            IApiService apiService,
            IStateService stateService
            )
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
            _stateService = stateService;
        }

        public async Task<string> GenerateUsernameAsync(UsernameGenerationOptions options)
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
            string randomWord = null;
            if (options.RandomWordUsernameCapitalize == null)
            {
                options.RandomWordUsernameCapitalize = false;
            }
            if (options.RandomWordUsernameIncludeNumber == null)
            {
                options.RandomWordUsernameIncludeNumber = false;
            }
            var listLength = EEFLongWordList.Instance.List.Count - 1;
            var wordIndex = await _cryptoService.RandomNumberAsync(0, listLength);
            if (options.RandomWordUsernameCapitalize.GetValueOrDefault())
            {
                randomWord = Capitalize(EEFLongWordList.Instance.List[wordIndex]);
            }
            else
            {
                randomWord = EEFLongWordList.Instance.List[wordIndex];
            }

            if (options.RandomWordUsernameIncludeNumber.GetValueOrDefault())
            {
                randomWord = await AppendRandomNumberToRandomWordAsync(randomWord);
            }

            return randomWord;
        }

        private async Task<string> GeneratePlusAddressedEmailAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);
            string generatedString = string.Empty;
            var adressedEmail = options.PlusAddressedEmail;

            if (string.IsNullOrWhiteSpace(adressedEmail) || adressedEmail.Length < 3)
            {
                return DefaultGenerated;
            }

            var atIndex = adressedEmail.IndexOf("@");
            if (atIndex < 1 || atIndex >= adressedEmail.Length - 1)
            {
                return adressedEmail;
            }

            var emailBeginning = adressedEmail.Substring(0, atIndex);
            var emailEnding = string.Empty;

            emailEnding = adressedEmail.Substring(atIndex + 1, adressedEmail.Length - (atIndex + 1));

            if (options.PlusAddressedEmailType == UsernameEmailType.Random)
            {
                generatedString = await RandomStringAsync(8);
            }
            else
            {
                generatedString = options.EmailWebsite;
            }

            return emailBeginning + "+" + generatedString + "@" + emailEnding;
        }

        private async Task<string> GenerateCatchAllAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);
            var catchAllEmailDomain = options.CatchAllEmailDomain;
            string generatedString = string.Empty;

            if (string.IsNullOrWhiteSpace(catchAllEmailDomain))
            {
                return DefaultGenerated;
            }

            if (options.CatchAllEmailType == UsernameEmailType.Random)
            {
                generatedString = await RandomStringAsync(8);
            }
            else
            {
                generatedString = options.EmailWebsite;
            }

            return generatedString + "@" + catchAllEmailDomain;
        }

        private async Task<string> GenerateForwardedEmailAliasAsync(UsernameGenerationOptions options)
        {
            options.Merge(_defaultOptions);

            switch (options.ServiceType)
            {
                case ForwardedEmailServiceType.AnonAddy:
                    if (string.IsNullOrWhiteSpace(options.AnonAddyApiAccessToken) || string.IsNullOrWhiteSpace(options.AnonAddyDomainName))
                    {
                        return DefaultGenerated;
                    }
                    return await GetAnonAddyUsername(options.AnonAddyApiAccessToken, options.AnonAddyDomainName);

                case ForwardedEmailServiceType.FirefoxRelay:
                    if (string.IsNullOrWhiteSpace(options.FirefoxRelayApiAccessToken))
                    {
                        return DefaultGenerated;
                    }
                    return await GetFirefoxRelayUsername(options.FirefoxRelayApiAccessToken);

                case ForwardedEmailServiceType.SimpleLogin:
                    if (string.IsNullOrWhiteSpace(options.SimpleLoginApiKey))
                    {
                        return DefaultGenerated;
                    }
                    return await GetSimpleLoginUsername(options.SimpleLoginApiKey);
                default:
                    return DefaultGenerated;
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
            return str.First().ToString().ToUpper() + str.Substring(1);
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
