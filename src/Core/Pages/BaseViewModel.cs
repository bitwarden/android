using Bit.App.Controls;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public abstract class BaseViewModel : ExtendedViewModel
    {
        private string _pageTitle = string.Empty;
        private AvatarImageSource _avatar;

        public string PageTitle
        {
            get => _pageTitle;
            set => SetProperty(ref _pageTitle, value);
        }

        public AvatarImageSource AvatarImageSource
        {
            get => _avatar ?? new AvatarImageSource();
            set => SetProperty(ref _avatar, value);
        }

        public ContentPage Page { get; set; }

        protected async Task<bool> HasConnectivityAsync()
        {
            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await _platformUtilsService.Value.ShowDialogAsync(
                    AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            return true;
        }
    }
}
