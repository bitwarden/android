using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GeneratorPageViewModel : BaseViewModel
    {
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IClipboardService _clipboardService;
        private readonly IUsernameGenerationService _usernameGenerationService;
        private readonly ITokenService _tokenService;
        private readonly IDeviceActionService _deviceActionService;
        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        private PasswordGenerationOptions _options;
        private UsernameGenerationOptions _usernameOptions;
        private PasswordGeneratorPolicyOptions _enforcedPolicyOptions;
        private string _password;
        private bool _isPassword;
        private bool _isUsername;
        private bool _uppercase;
        private bool _lowercase;
        private bool _number;
        private bool _special;
        private bool _allowAmbiguousChars;
        private int _minNumber;
        private int _minSpecial;
        private int _length = 5;
        private int _numWords = 3;
        private string _wordSeparator;
        private bool _capitalize;
        private bool _includeNumber;
        private string _username;
        private GeneratorType _generatorTypeSelected;
        private int _passwordTypeSelectedIndex;
        private bool _doneIniting;
        private bool _showTypePicker;
        private string _emailWebsite;
        private bool _showForwardedEmailApiSecret;
        private bool _editMode;

        public GeneratorPageViewModel()
        {
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _clipboardService = ServiceContainer.Resolve<IClipboardService>();
            _usernameGenerationService = ServiceContainer.Resolve<IUsernameGenerationService>();
            _tokenService = ServiceContainer.Resolve<ITokenService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();

            PageTitle = AppResources.Generator;
            GeneratorTypeOptions = new List<GeneratorType> {
                GeneratorType.Password,
                GeneratorType.Username
            };
            PasswordTypeOptions = new List<string> { AppResources.Password, AppResources.Passphrase };

            UsernameTypeOptions = new List<UsernameType> {
                UsernameType.PlusAddressedEmail,
                UsernameType.CatchAllEmail,
                UsernameType.ForwardedEmailAlias,
                UsernameType.RandomWord
            };

            ForwardedEmailServiceTypeOptions = new List<ForwardedEmailServiceType> {
                ForwardedEmailServiceType.AnonAddy,
                ForwardedEmailServiceType.DuckDuckGo,
                ForwardedEmailServiceType.Fastmail,
                ForwardedEmailServiceType.FirefoxRelay,
                ForwardedEmailServiceType.ForwardEmail,
                ForwardedEmailServiceType.SimpleLogin
            };

            UsernameEmailTypeOptions = new List<UsernameEmailType>
            {
                UsernameEmailType.Random,
                UsernameEmailType.Website
            };

            UsernameTypePromptHelpCommand = new Command(UsernameTypePromptHelp);
            RegenerateCommand = new AsyncCommand(RegenerateAsync, onException: ex => OnSubmitException(ex), allowsMultipleExecutions: false);
            RegenerateUsernameCommand = new AsyncCommand(RegenerateUsernameAsync, onException: ex => OnSubmitException(ex), allowsMultipleExecutions: false);
            ToggleForwardedEmailHiddenValueCommand = new Command(() => ShowForwardedEmailApiSecret = !ShowForwardedEmailApiSecret);
            CopyCommand = new AsyncCommand(CopyAsync, onException: ex => _logger.Value.Exception(ex), allowsMultipleExecutions: false);
            CloseCommand = new AsyncCommand(CloseAsync, onException: ex => _logger.Value.Exception(ex), allowsMultipleExecutions: false);
        }

        public List<GeneratorType> GeneratorTypeOptions { get; set; }
        public List<string> PasswordTypeOptions { get; set; }
        public List<UsernameType> UsernameTypeOptions { get; set; }
        public List<ForwardedEmailServiceType> ForwardedEmailServiceTypeOptions { get; set; }
        public List<UsernameEmailType> UsernameEmailTypeOptions { get; set; }

        public Command UsernameTypePromptHelpCommand { get; set; }
        public ICommand RegenerateCommand { get; set; }
        public ICommand RegenerateUsernameCommand { get; set; }
        public ICommand ToggleForwardedEmailHiddenValueCommand { get; set; }
        public ICommand CopyCommand { get; set; }
        public ICommand CloseCommand { get; set; }

        public string Password
        {
            get => _password;
            set => SetProperty(ref _password, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ColoredPassword)
                });
        }

        public string Username
        {
            get => _username;
            set => SetProperty(ref _username, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ColoredUsername)
                });
        }

        public string ColoredPassword => GeneratedValueFormatter.Format(Password);
        public string ColoredUsername => GeneratedValueFormatter.Format(Username);

        public bool IsPassword
        {
            get => _isPassword;
            set => SetProperty(ref _isPassword, value);
        }

        public bool IsUsername
        {
            get => _isUsername;
            set => SetProperty(ref _isUsername, value);
        }

        public bool IosExtension { get; set; }

        public bool ShowTypePicker
        {
            get => _showTypePicker;
            set => SetProperty(ref _showTypePicker, value);
        }

        public bool EditMode
        {
            get => _editMode;
            set => SetProperty(ref _editMode, value, additionalPropertyNames: new string[]
                    {
                        nameof(ShowUsernameEmailType)
                    });
        }

        public bool ShowUsernameEmailType
        {
            get => !string.IsNullOrWhiteSpace(EmailWebsite);
        }

        public int Length
        {
            get => _length;
            set
            {
                if (SetProperty(ref _length, value))
                {
                    _options.Length = value;
                    var task = SliderInputAsync();
                }
            }
        }

        public bool Uppercase
        {
            get => _uppercase;
            set
            {
                if (SetProperty(ref _uppercase, value))
                {
                    _options.Uppercase = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool Lowercase
        {
            get => _lowercase;
            set
            {
                if (SetProperty(ref _lowercase, value))
                {
                    _options.Lowercase = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool Number
        {
            get => _number;
            set
            {
                if (SetProperty(ref _number, value))
                {
                    _options.Number = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool Special
        {
            get => _special;
            set
            {
                if (SetProperty(ref _special, value))
                {
                    _options.Special = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool AllowAmbiguousChars
        {
            get => _allowAmbiguousChars;
            set
            {
                if (SetProperty(ref _allowAmbiguousChars, value,
                    additionalPropertyNames: new string[]
                    {
                        nameof(AvoidAmbiguousChars)
                    }))
                {
                    _options.AllowAmbiguousChar = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool AvoidAmbiguousChars
        {
            get => !AllowAmbiguousChars;
            set => AllowAmbiguousChars = !value;
        }

        public int MinNumber
        {
            get => _minNumber;
            set
            {
                if (SetProperty(ref _minNumber, value))
                {
                    _options.MinNumber = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public int MinSpecial
        {
            get => _minSpecial;
            set
            {
                if (SetProperty(ref _minSpecial, value))
                {
                    _options.MinSpecial = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public int NumWords
        {
            get => _numWords;
            set
            {
                if (SetProperty(ref _numWords, value))
                {
                    _options.NumWords = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public string WordSeparator
        {
            get => _wordSeparator;
            set
            {
                if (value == null)
                {
                    return;
                }
                var val = value.Trim();
                if (SetProperty(ref _wordSeparator, val))
                {
                    _options.WordSeparator = val;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool Capitalize
        {
            get => _capitalize;
            set
            {
                if (SetProperty(ref _capitalize, value))
                {
                    _options.Capitalize = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool IncludeNumber
        {
            get => _includeNumber;
            set
            {
                if (SetProperty(ref _includeNumber, value))
                {
                    _options.Number = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public string PlusAddressedEmail
        {
            get => _usernameOptions.PlusAddressedEmail;
            set
            {
                if (_usernameOptions.PlusAddressedEmail != value)
                {
                    _usernameOptions.PlusAddressedEmail = value;
                    TriggerPropertyChanged(nameof(PlusAddressedEmail));
                    SaveUsernameOptionsAsync(false).FireAndForget();
                }
            }
        }

        public PasswordGeneratorPolicyOptions EnforcedPolicyOptions
        {
            get => _enforcedPolicyOptions;
            set => SetProperty(ref _enforcedPolicyOptions, value,
                additionalPropertyNames: new[]
                {
                    nameof(IsPolicyInEffect)
                });
        }

        public bool IsPolicyInEffect => _enforcedPolicyOptions.InEffect();

        public GeneratorType GeneratorTypeSelected
        {
            get => _generatorTypeSelected;
            set
            {
                if (SetProperty(ref _generatorTypeSelected, value))
                {
                    IsUsername = value == GeneratorType.Username;
                    TriggerPropertyChanged(nameof(GeneratorTypeSelected));
                    SaveOptionsAsync().FireAndForget();
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public int PasswordTypeSelectedIndex
        {
            get => _passwordTypeSelectedIndex;
            set
            {
                if (SetProperty(ref _passwordTypeSelectedIndex, value))
                {
                    IsPassword = value == 0;
                    TriggerPropertyChanged(nameof(PasswordTypeSelectedIndex));
                    SaveOptionsAsync().FireAndForget();
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public UsernameType UsernameTypeSelected
        {
            get => _usernameOptions.Type;
            set
            {
                if (_usernameOptions.Type != value)
                {
                    _usernameOptions.Type = value;
                    Username = Constants.DefaultUsernameGenerated;
                    TriggerPropertyChanged(nameof(UsernameTypeSelected), new string[] { nameof(UsernameTypeDescriptionLabel) });
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public string UsernameTypeDescriptionLabel => GetUsernameTypeLabelDescription(UsernameTypeSelected);

        public ForwardedEmailServiceType ForwardedEmailServiceSelected
        {
            get => _usernameOptions.ServiceType;
            set
            {
                if (_usernameOptions.ServiceType != value)
                {
                    _usernameOptions.ServiceType = value;
                    Username = Constants.DefaultUsernameGenerated;
                    TriggerPropertyChanged(nameof(ForwardedEmailServiceSelected), new string[]
                    {
                        nameof(ForwardedEmailApiSecret),
                        nameof(ForwardedEmailApiSecretLabel)
                    });
                    SaveUsernameOptionsAsync(false).FireAndForget();
                }
            }
        }

        public string CatchAllEmailDomain
        {
            get => _usernameOptions.CatchAllEmailDomain;
            set
            {
                if (_usernameOptions.CatchAllEmailDomain != value)
                {
                    _usernameOptions.CatchAllEmailDomain = value;
                    TriggerPropertyChanged(nameof(CatchAllEmailDomain));
                    SaveUsernameOptionsAsync(false).FireAndForget();
                }
            }
        }

        public string ForwardedEmailApiSecret
        {
            get
            {
                switch (ForwardedEmailServiceSelected)
                {
                    case ForwardedEmailServiceType.AnonAddy:
                        return _usernameOptions.AnonAddyApiAccessToken;
                    case ForwardedEmailServiceType.DuckDuckGo:
                        return _usernameOptions.DuckDuckGoApiKey;
                    case ForwardedEmailServiceType.Fastmail:
                        return _usernameOptions.FastMailApiKey;
                    case ForwardedEmailServiceType.FirefoxRelay:
                        return _usernameOptions.FirefoxRelayApiAccessToken;
                    case ForwardedEmailServiceType.SimpleLogin:
                        return _usernameOptions.SimpleLoginApiKey;
                    case ForwardedEmailServiceType.ForwardEmail:
                        return _usernameOptions.ForwardEmailApiAccessToken;
                    default:
                        return null;
                }
            }
            set
            {
                bool changed = false;
                switch (ForwardedEmailServiceSelected)
                {
                    case ForwardedEmailServiceType.AnonAddy:
                        if (_usernameOptions.AnonAddyApiAccessToken != value)
                        {
                            _usernameOptions.AnonAddyApiAccessToken = value;
                            changed = true;
                        }
                        break;
                    case ForwardedEmailServiceType.DuckDuckGo:
                        if (_usernameOptions.DuckDuckGoApiKey != value)
                        {
                            _usernameOptions.DuckDuckGoApiKey = value;
                            changed = true;
                        }
                        break;
                    case ForwardedEmailServiceType.Fastmail:
                        if (_usernameOptions.FastMailApiKey != value)
                        {
                            _usernameOptions.FastMailApiKey = value;
                            changed = true;
                        }
                        break;
                    case ForwardedEmailServiceType.FirefoxRelay:
                        if (_usernameOptions.FirefoxRelayApiAccessToken != value)
                        {
                            _usernameOptions.FirefoxRelayApiAccessToken = value;
                            changed = true;
                        }
                        break;
                    case ForwardedEmailServiceType.SimpleLogin:
                        if (_usernameOptions.SimpleLoginApiKey != value)
                        {
                            _usernameOptions.SimpleLoginApiKey = value;
                            changed = true;
                        }
                        break;

                    case ForwardedEmailServiceType.ForwardEmail:
                        if (_usernameOptions.ForwardEmailApiAccessToken != value)
                        {
                            _usernameOptions.ForwardEmailApiAccessToken = value;
                            changed = true;
                        }
                        break;
                    default:
                        break;
                }

                if (changed)
                {
                    TriggerPropertyChanged(nameof(ForwardedEmailApiSecret));
                    SaveUsernameOptionsAsync(false).FireAndForget();
                }
            }
        }

        public string ForwardedEmailApiSecretLabel
        {
            get
            {
                switch (ForwardedEmailServiceSelected)
                {
                    case ForwardedEmailServiceType.AnonAddy:
                    case ForwardedEmailServiceType.FirefoxRelay:
                        return AppResources.APIAccessToken;
                    case ForwardedEmailServiceType.DuckDuckGo:
                    case ForwardedEmailServiceType.Fastmail:
                    case ForwardedEmailServiceType.SimpleLogin:
                    case ForwardedEmailServiceType.ForwardEmail:
                        return AppResources.APIKeyRequiredParenthesis;
                    default:
                        return null;
                }
            }
        }

        public bool ShowForwardedEmailApiSecret
        {
            get
            {
                return _showForwardedEmailApiSecret;
            }
            set => SetProperty(ref _showForwardedEmailApiSecret, value);
        }

        public string AddyIoDomainName
        {
            get => _usernameOptions.AnonAddyDomainName;
            set
            {
                if (_usernameOptions.AnonAddyDomainName != value)
                {
                    _usernameOptions.AnonAddyDomainName = value;
                    TriggerPropertyChanged(nameof(AddyIoDomainName));
                    SaveUsernameOptionsAsync(false).FireAndForget();
                }
            }
        }

        public string ForwardEmailDomainName
        {
            get => _usernameOptions.ForwardEmailDomainName;
            set
            {
                if (_usernameOptions.ForwardEmailDomainName != value)
                {
                    _usernameOptions.ForwardEmailDomainName = value;
                    TriggerPropertyChanged(nameof(ForwardEmailDomainName));
                    SaveUsernameOptionsAsync(false).FireAndForget();
                }
            }
        }

        public bool CapitalizeRandomWordUsername
        {
            get => _usernameOptions.CapitalizeRandomWordUsername;
            set
            {
                if (_usernameOptions.CapitalizeRandomWordUsername != value)
                {
                    _usernameOptions.CapitalizeRandomWordUsername = value;
                    TriggerPropertyChanged(nameof(CapitalizeRandomWordUsername));
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public bool IncludeNumberRandomWordUsername
        {
            get => _usernameOptions.IncludeNumberRandomWordUsername;
            set
            {
                if (_usernameOptions.IncludeNumberRandomWordUsername != value)
                {
                    _usernameOptions.IncludeNumberRandomWordUsername = value;
                    TriggerPropertyChanged(nameof(IncludeNumberRandomWordUsername));
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public UsernameEmailType PlusAddressedEmailTypeSelected
        {
            get => _usernameOptions.PlusAddressedEmailType;
            set
            {
                if (_usernameOptions.PlusAddressedEmailType != value)
                {
                    _usernameOptions.PlusAddressedEmailType = value;
                    TriggerPropertyChanged(nameof(PlusAddressedEmailTypeSelected));
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public UsernameEmailType CatchAllEmailTypeSelected
        {
            get => _usernameOptions.CatchAllEmailType;
            set
            {
                if (_usernameOptions.CatchAllEmailType != value)
                {
                    _usernameOptions.CatchAllEmailType = value;
                    TriggerPropertyChanged(nameof(CatchAllEmailTypeSelected));
                    SaveUsernameOptionsAsync().FireAndForget();
                }
            }
        }

        public string EmailWebsite
        {
            get => _emailWebsite;
            set => SetProperty(ref _emailWebsite, value, additionalPropertyNames: new string[]
                    {
                        nameof(ShowUsernameEmailType)
                    });
        }

        public async Task InitAsync()
        {
            (_options, EnforcedPolicyOptions) = await _passwordGenerationService.GetOptionsAsync();
            LoadFromOptions();

            _usernameOptions = await _usernameGenerationService.GetOptionsAsync();
            await _tokenService.PrepareTokenForDecodingAsync();
            _usernameOptions.PlusAddressedEmail = _tokenService.GetEmail();
            _usernameOptions.EmailWebsite = EmailWebsite;
            _usernameOptions.CatchAllEmailType = _usernameOptions.PlusAddressedEmailType = string.IsNullOrWhiteSpace(EmailWebsite) || !EditMode ? UsernameEmailType.Random : UsernameEmailType.Website;

            if (!IsUsername)
            {
                await RegenerateAsync();
            }
            else
            {
                if (UsernameTypeSelected != UsernameType.ForwardedEmailAlias)
                {
                    await RegenerateUsernameAsync();
                }
                else
                {
                    Username = Constants.DefaultUsernameGenerated;
                }
            }
            TriggerUsernamePropertiesChanged();

            _doneIniting = true;
        }

        public async Task RegenerateAsync()
        {
            Password = await _passwordGenerationService.GeneratePasswordAsync(_options);
            await _passwordGenerationService.AddHistoryAsync(Password);
        }

        public async Task RegenerateUsernameAsync()
        {
            Username = await _usernameGenerationService.GenerateAsync(_usernameOptions);
        }

        public void RedrawPassword()
        {
            if (!string.IsNullOrEmpty(_password))
            {
                TriggerPropertyChanged(nameof(ColoredPassword));
            }
        }

        public void RedrawUsername()
        {
            if (!string.IsNullOrEmpty(_username))
            {
                TriggerPropertyChanged(nameof(ColoredUsername));
            }
        }

        public async Task SaveOptionsAsync(bool regenerate = true)
        {
            if (!_doneIniting)
            {
                return;
            }
            SetOptions();
            _passwordGenerationService.NormalizeOptions(_options, _enforcedPolicyOptions);
            await _passwordGenerationService.SaveOptionsAsync(_options);

            LoadFromOptions();
            if (regenerate)
            {
                await RegenerateAsync();
            }
        }

        public async Task SaveUsernameOptionsAsync(bool regenerate = true)
        {
            if (!_doneIniting)
            {
                return;
            }

            _usernameOptions.EmailWebsite = EmailWebsite;
            await _usernameGenerationService.SaveOptionsAsync(_usernameOptions);

            if (regenerate && UsernameTypeSelected != UsernameType.ForwardedEmailAlias)
            {
                await RegenerateUsernameAsync();
            }
            else
            {
                Username = Constants.DefaultUsernameGenerated;
            }
        }

        public async Task SliderChangedAsync()
        {
            await SaveOptionsAsync(false);
            await _passwordGenerationService.AddHistoryAsync(Password);
        }

        public async Task SliderInputAsync()
        {
            if (!_doneIniting)
            {
                return;
            }
            SetOptions();
            _passwordGenerationService.NormalizeOptions(_options, _enforcedPolicyOptions);
            Password = await _passwordGenerationService.GeneratePasswordAsync(_options);
        }

        public async Task CopyAsync()
        {
            await _clipboardService.CopyTextAsync(IsUsername ? Username : Password);
            _platformUtilsService.ShowToastForCopiedValue(IsUsername ? AppResources.Username : AppResources.Password);
        }

        public void UsernameTypePromptHelp()
        {
            try
            {
                _platformUtilsService.LaunchUri("https://bitwarden.com/help/generator/#username-types");
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
                Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok);
            }
        }

        public async Task CloseAsync()
        {
            if (IosExtension)
            {
                _deviceActionService.CloseExtensionPopUp();
            }
            else
            {
                await Page.Navigation.PopModalAsync();
            }
        }

        private void LoadFromOptions()
        {
            AllowAmbiguousChars = _options.AllowAmbiguousChar.GetValueOrDefault();
            PasswordTypeSelectedIndex = _options.Type == "passphrase" ? 1 : 0;
            IsPassword = PasswordTypeSelectedIndex == 0;
            MinNumber = _options.MinNumber.GetValueOrDefault();
            MinSpecial = _options.MinSpecial.GetValueOrDefault();
            Special = _options.Special.GetValueOrDefault();
            Number = _options.Number.GetValueOrDefault();
            NumWords = _options.NumWords.GetValueOrDefault();
            WordSeparator = _options.WordSeparator;
            Uppercase = _options.Uppercase.GetValueOrDefault();
            Lowercase = _options.Lowercase.GetValueOrDefault();
            Length = _options.Length.GetValueOrDefault(5);
            Capitalize = _options.Capitalize.GetValueOrDefault();
            IncludeNumber = _options.IncludeNumber.GetValueOrDefault();
        }

        private void TriggerUsernamePropertiesChanged()
        {
            TriggerPropertyChanged(nameof(CatchAllEmailTypeSelected));
            TriggerPropertyChanged(nameof(PlusAddressedEmailTypeSelected));
            TriggerPropertyChanged(nameof(IncludeNumberRandomWordUsername));
            TriggerPropertyChanged(nameof(CapitalizeRandomWordUsername));
            TriggerPropertyChanged(nameof(ForwardedEmailApiSecret));
            TriggerPropertyChanged(nameof(ForwardedEmailApiSecretLabel));
            TriggerPropertyChanged(nameof(AddyIoDomainName));
            TriggerPropertyChanged(nameof(CatchAllEmailDomain));
            TriggerPropertyChanged(nameof(ForwardedEmailServiceSelected));
            TriggerPropertyChanged(nameof(UsernameTypeSelected));
            TriggerPropertyChanged(nameof(PasswordTypeSelectedIndex));
            TriggerPropertyChanged(nameof(GeneratorTypeSelected));
            TriggerPropertyChanged(nameof(PlusAddressedEmail));
            TriggerPropertyChanged(nameof(GeneratorTypeSelected));
            TriggerPropertyChanged(nameof(UsernameTypeDescriptionLabel));
            TriggerPropertyChanged(nameof(EmailWebsite));
            TriggerPropertyChanged(nameof(ForwardEmailDomainName));
        }

        private void SetOptions()
        {
            _options.AllowAmbiguousChar = AllowAmbiguousChars;
            _options.Type = PasswordTypeSelectedIndex == 1 ? PasswordGenerationOptions.TYPE_PASSPHRASE : PasswordGenerationOptions.TYPE_PASSWORD;
            _options.MinNumber = MinNumber;
            _options.MinSpecial = MinSpecial;
            _options.Special = Special;
            _options.NumWords = NumWords;
            _options.Number = Number;
            _options.WordSeparator = WordSeparator;
            _options.Uppercase = Uppercase;
            _options.Lowercase = Lowercase;
            _options.Length = Length;
            _options.Capitalize = Capitalize;
            _options.IncludeNumber = IncludeNumber;
        }

        private async void OnSubmitException(Exception ex)
        {
            _logger.Value.Exception(ex);

            string message = AppResources.GenericErrorMessage;

            if (IsUsername && UsernameTypeSelected == UsernameType.ForwardedEmailAlias)
            {
                if (ex is ForwardedEmailInvalidSecretException)
                {
                    message = ForwardedEmailServiceSelected == ForwardedEmailServiceType.AnonAddy || ForwardedEmailServiceSelected == ForwardedEmailServiceType.FirefoxRelay
                                ? AppResources.InvalidAPIToken
                                : AppResources.InvalidAPIKey;
                }
                else
                {
                    message = string.Format(AppResources.UnknownXErrorMessage, ForwardedEmailServiceSelected);
                }
            }

            await Device.InvokeOnMainThreadAsync(() => Page.DisplayAlert(AppResources.AnErrorHasOccurred, message, AppResources.Ok));
        }

        private string GetUsernameTypeLabelDescription(UsernameType value)
        {
            switch (value)
            {
                case UsernameType.PlusAddressedEmail:
                    return AppResources.PlusAddressedEmailDescription;
                case UsernameType.CatchAllEmail:
                    return AppResources.CatchAllEmailDescription;
                case UsernameType.ForwardedEmailAlias:
                    return AppResources.ForwardedEmailDescription;
                default:
                    return string.Empty;
            }
        }
    }
}
