using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class GeneratorPageViewModel : BaseViewModel
    {
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private PasswordGenerationOptions _options;
        private string _password;
        private bool _isPassword;
        private int _typeSelectedIndex;

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
            set => SetProperty(ref _password, value);
        }

        public bool IsPassword
        {
            get => _isPassword;
            set => SetProperty(ref _isPassword, value);
        }

        public PasswordGenerationOptions Options
        {
            get => _options;
            set => SetProperty(ref _options, value);
        }

        public int TypeSelectedIndex
        {
            get => _typeSelectedIndex;
            set
            {
                if(SetProperty(ref _typeSelectedIndex, value))
                {
                    TypeChanged();
                }
            }
        }

        public async Task InitAsync()
        {
            Options = await _passwordGenerationService.GetOptionsAsync();
            TypeSelectedIndex = Options.Type == "passphrase" ? 1 : 0;
            Password = await _passwordGenerationService.GeneratePasswordAsync(Options);
            await _passwordGenerationService.AddHistoryAsync(Password);
        }

        public async Task RegenerateAsync()
        {
            Password = await _passwordGenerationService.GeneratePasswordAsync(Options);
            await _passwordGenerationService.AddHistoryAsync(Password);
        }

        public async Task SaveOptionsAsync(bool regenerate = true)
        {
            _passwordGenerationService.NormalizeOptions(Options);
            await _passwordGenerationService.SaveOptionsAsync(Options);
            if(regenerate)
            {
                await RegenerateAsync();
            }
        }

        public async Task CopyAsync()
        {
            await _platformUtilsService.CopyToClipboardAsync(Password);
            _platformUtilsService.ShowToast("success", null, AppResources.CopiedPassword);
        }

        public async void TypeChanged()
        {
            IsPassword = TypeSelectedIndex == 0;
            Options.Type = IsPassword ? "password" : "passphrase";
            await SaveOptionsAsync();
        }
    }
}
