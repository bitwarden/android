using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class GeneratorPageViewModel : BaseViewModel
    {
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private PasswordGenerationOptions _options;
        private string _password;
        private bool _isPassword;
        private bool _uppercase;
        private bool _lowercase;
        private bool _number;
        private bool _special;
        private bool _avoidAmbiguous;
        private int _minNumber;
        private int _minSpecial;
        private int _length = 5;
        private int _numWords = 3;
        private string _wordSeparator;
        private int _typeSelectedIndex;
        private bool _doneIniting;
        private CancellationTokenSource _sliderCancellationTokenSource;

        public GeneratorPageViewModel()
        {
            _passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>(
                "passwordGenerationService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            PageTitle = AppResources.PasswordGenerator;
            TypeOptions = new List<string> { AppResources.Password, AppResources.Passphrase };
        }

        public List<string> TypeOptions { get; set; }

        public string Password
        {
            get => _password;
            set => SetProperty(ref _password, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ColoredPassword)
                });
        }

        public FormattedString ColoredPassword => PasswordFormatter.FormatPassword(Password);

        public bool IsPassword
        {
            get => _isPassword;
            set => SetProperty(ref _isPassword, value);
        }

        public int Length
        {
            get => _length;
            set
            {
                if(SetProperty(ref _length, value))
                {
                    _options.Length = value;
                    var task = SaveOptionsSliderAsync();
                }
            }
        }

        public bool Uppercase
        {
            get => _uppercase;
            set
            {
                if(SetProperty(ref _uppercase, value))
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
                if(SetProperty(ref _lowercase, value))
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
                if(SetProperty(ref _number, value))
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
                if(SetProperty(ref _special, value))
                {
                    _options.Special = value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public bool AvoidAmbiguous
        {
            get => _avoidAmbiguous;
            set
            {
                if(SetProperty(ref _avoidAmbiguous, value))
                {
                    _options.Ambiguous = !value;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public int MinNumber
        {
            get => _minNumber;
            set
            {
                if(SetProperty(ref _minNumber, value))
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
                if(SetProperty(ref _minSpecial, value))
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
                if(SetProperty(ref _numWords, value))
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
                var val = value.Trim();
                if(SetProperty(ref _wordSeparator, val))
                {
                    _options.WordSeparator = val;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public int TypeSelectedIndex
        {
            get => _typeSelectedIndex;
            set
            {
                if(SetProperty(ref _typeSelectedIndex, value))
                {
                    IsPassword = value == 0;
                    var task = SaveOptionsAsync();
                }
            }
        }

        public async Task InitAsync()
        {
            _options = await _passwordGenerationService.GetOptionsAsync();
            LoadFromOptions();
            await RegenerateAsync();
            _doneIniting = true;
        }

        public async Task RegenerateAsync()
        {
            Password = await _passwordGenerationService.GeneratePasswordAsync(_options);
            await _passwordGenerationService.AddHistoryAsync(Password);
        }

        public async Task SaveOptionsAsync(bool regenerate = true)
        {
            if(!_doneIniting)
            {
                return;
            }
            SetOptions();
            _passwordGenerationService.NormalizeOptions(_options);
            await _passwordGenerationService.SaveOptionsAsync(_options);
            LoadFromOptions();
            if(regenerate)
            {
                await RegenerateAsync();
            }
        }

        public async Task SaveOptionsSliderAsync()
        {
            if(!_doneIniting)
            {
                return;
            }
            SetOptions();
            _passwordGenerationService.NormalizeOptions(_options);
            LoadFromOptions();
            Password = await _passwordGenerationService.GeneratePasswordAsync(_options);

            var page = Page as GeneratorPage;
            var previousCts = _sliderCancellationTokenSource;
            var cts = new CancellationTokenSource();
            var task = Task.Run(async () =>
            {
                await Task.Delay(500);
                if(DateTime.UtcNow - page.LastLengthSliderChange < TimeSpan.FromMilliseconds(450))
                {
                    return;
                }
                else
                {
                    previousCts?.Cancel();
                }
                cts.Token.ThrowIfCancellationRequested();
                await _passwordGenerationService.SaveOptionsAsync(_options);
                cts.Token.ThrowIfCancellationRequested();
                await _passwordGenerationService.AddHistoryAsync(Password, cts.Token);
            }, cts.Token);
            _sliderCancellationTokenSource = cts;
        }

        public async Task CopyAsync()
        {
            await _platformUtilsService.CopyToClipboardAsync(Password);
            _platformUtilsService.ShowToast("success", null, AppResources.CopiedPassword);
        }

        private void LoadFromOptions()
        {
            AvoidAmbiguous = !_options.Ambiguous.GetValueOrDefault();
            TypeSelectedIndex = _options.Type == "passphrase" ? 1 : 0;
            IsPassword = TypeSelectedIndex == 0;
            MinNumber = _options.MinNumber.GetValueOrDefault();
            MinSpecial = _options.MinSpecial.GetValueOrDefault();
            Special = _options.Special.GetValueOrDefault();
            Number = _options.Number.GetValueOrDefault();
            NumWords = _options.NumWords.GetValueOrDefault();
            WordSeparator = _options.WordSeparator;
            Uppercase = _options.Uppercase.GetValueOrDefault();
            Lowercase = _options.Lowercase.GetValueOrDefault();
            Length = _options.Length.GetValueOrDefault(5);
        }

        private void SetOptions()
        {
            _options.Ambiguous = !AvoidAmbiguous;
            _options.Type = TypeSelectedIndex == 1 ? "passphrase" : "password";
            _options.MinNumber = MinNumber;
            _options.MinSpecial = MinSpecial;
            _options.Special = Special;
            _options.NumWords = NumWords;
            _options.Number = Number;
            _options.WordSeparator = WordSeparator;
            _options.Uppercase = Uppercase;
            _options.Lowercase = Lowercase;
            _options.Length = Length;
        }
    }
}
