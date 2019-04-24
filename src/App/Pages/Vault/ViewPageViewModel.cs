using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class ViewPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly ICipherService _cipherService;
        private readonly IUserService _userService;
        private CipherView _cipher;
        private bool _canAccessPremium;

        public ViewPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");

            PageTitle = AppResources.ViewItem;
        }

        public string  CipherId { get; set; }
        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value);
        }
        public bool CanAccessPremium
        {
            get => _canAccessPremium;
            set => SetProperty(ref _canAccessPremium, value);
        }

        public async Task LoadAsync()
        {
            // TODO: Cleanup

            var cipher = await _cipherService.GetAsync(CipherId);
            Cipher = await cipher.DecryptAsync();
            CanAccessPremium = await _userService.CanAccessPremiumAsync();

            // TODO: Totp
        }
    }
}
