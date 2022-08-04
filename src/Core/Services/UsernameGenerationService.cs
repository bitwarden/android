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
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private UsernameGenerationOptions _defaultOptions = new UsernameGenerationOptions(true);

        public UsernameGenerationService(
            ICryptoService cryptoService,
            IApiService apiService
            )
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
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

            if(string.IsNullOrWhiteSpace(adressedEmail) || adressedEmail.Length < 3)
            {
                return adressedEmail;
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
                generatedString = options.PlusAddressedEmailWebsite;
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
                return catchAllEmailDomain;
            }

            if(options.CatchAllEmailType == UsernameEmailType.Random)
            {
                generatedString = await RandomStringAsync(8);
            }
            else
            {
                generatedString = options.CatchAllEmailWebsite;
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
                        return string.Empty;
                    }
                    return await GetAnonAddyUsername(options.AnonAddyApiAccessToken, options.AnonAddyDomainName);

                case ForwardedEmailServiceType.FirefoxRelay:
                    if (string.IsNullOrWhiteSpace(options.FirefoxRelayApiAccessToken))
                    {
                        return string.Empty;
                    }
                    return await GetFirefoxRelayUsername(options.FirefoxRelayApiAccessToken);

                case ForwardedEmailServiceType.SimpleLogin:
                    if (string.IsNullOrWhiteSpace(options.SimpleLoginApiKey))
                    {
                        return string.Empty;
                    }
                    return await GetSimpleLoginUsername(options.SimpleLoginApiKey);
                default:
                    return string.Empty;
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
