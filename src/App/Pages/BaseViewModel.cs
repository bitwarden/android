using Bit.App.Abstractions;
using System;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

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

        protected void HandleException(Exception ex, IDeviceActionService deviceActionService, IPlatformUtilsService platformUtilsService, ILogger logger)
        {
            Xamarin.Essentials.MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await deviceActionService.HideLoadingAsync();
                await platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage);
            }).FireAndForget();
            logger.Exception(ex);
        }
    }
}
