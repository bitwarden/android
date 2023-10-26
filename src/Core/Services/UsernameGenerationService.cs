using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Services.EmailForwarders;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class UsernameGenerationService : IUsernameGenerationService
    {
        private const string CATCH_ALL_EMAIL_DOMAIN_FORMAT = "{0}@{1}";
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();
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
                    _logger.Value.Error($"Error UsernameGenerationService: UsernameType {options.Type} not implemented.");
                    return string.Empty;
            }
        }

        public async Task<UsernameGenerationOptions> GetOptionsAsync()
        {
            if (_optionsCache == null)
            {
                var options = await _stateService.GetUsernameGenerationOptionsAsync();
                _optionsCache = options ?? new UsernameGenerationOptions();
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
            var listLength = EEFLongWordList.Instance.List.Count - 1;
            var wordIndex = await _cryptoService.RandomNumberAsync(0, listLength);
            var randomWord = EEFLongWordList.Instance.List[wordIndex];

            if (string.IsNullOrWhiteSpace(randomWord))
            {
                _logger.Value.Error($"Error UsernameGenerationService: EEFLongWordList has NullOrWhiteSpace value at {wordIndex} index.");
                return Constants.DefaultUsernameGenerated;
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
            if (string.IsNullOrWhiteSpace(options.PlusAddressedEmail) || options.PlusAddressedEmail.Length < 3)
            {
                return Constants.DefaultUsernameGenerated;
            }

            var atIndex = options.PlusAddressedEmail.IndexOf("@");
            if (atIndex < 1 || atIndex >= options.PlusAddressedEmail.Length - 1)
            {
                return options.PlusAddressedEmail;
            }

            if (options.PlusAddressedEmailType == UsernameEmailType.Random)
            {
                var randomString = await _cryptoService.RandomStringAsync(8);
                return options.PlusAddressedEmail.Insert(atIndex, $"+{randomString}");
            }
            else
            {
                return options.PlusAddressedEmail.Insert(atIndex, $"+{options.EmailWebsite}");
            }
        }

        private async Task<string> GenerateCatchAllAsync(UsernameGenerationOptions options)
        {
            var catchAllEmailDomain = options.CatchAllEmailDomain;

            if (string.IsNullOrWhiteSpace(catchAllEmailDomain))
            {
                return Constants.DefaultUsernameGenerated;
            }

            if (options.CatchAllEmailType == UsernameEmailType.Random)
            {
                var randomString = await _cryptoService.RandomStringAsync(8);
                return string.Format(CATCH_ALL_EMAIL_DOMAIN_FORMAT, randomString, catchAllEmailDomain);
            }

            return string.Format(CATCH_ALL_EMAIL_DOMAIN_FORMAT, options.EmailWebsite, catchAllEmailDomain);
        }

        private async Task<string> GenerateForwardedEmailAliasAsync(UsernameGenerationOptions options)
        {
            if (options.ServiceType == ForwardedEmailServiceType.AnonAddy)
            {
                return await new AnonAddyForwarder()
                                    .GenerateAsync(_apiService, (AnonAddyForwarderOptions)options.GetForwarderOptions());
            }

            if (options.ServiceType == ForwardedEmailServiceType.ForwardEmail)
            {
                var forwardedEmailOptions = (ForwardEmailForwarderOptions)options.GetForwarderOptions();
                return await new ForwardEmailForwarder(forwardedEmailOptions.DomainName)
                                    .GenerateAsync(_apiService, forwardedEmailOptions);
            }

            BaseForwarder<ForwarderOptions> simpleForwarder = null;

            switch (options.ServiceType)
            {
                case ForwardedEmailServiceType.FirefoxRelay:
                    simpleForwarder = new FirefoxRelayForwarder();
                    break;
                case ForwardedEmailServiceType.SimpleLogin:
                    simpleForwarder = new SimpleLoginForwarder();
                    break;
                case ForwardedEmailServiceType.DuckDuckGo:
                    simpleForwarder = new DuckDuckGoForwarder();
                    break;
                case ForwardedEmailServiceType.Fastmail:
                    simpleForwarder = new FastmailForwarder();
                    break;
                default:
                    _logger.Value.Error($"Error UsernameGenerationService: ForwardedEmailServiceType {options.ServiceType} not implemented.");
                    return Constants.DefaultUsernameGenerated;
            }

            return await simpleForwarder.GenerateAsync(_apiService, options.GetForwarderOptions());
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
