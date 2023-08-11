using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public abstract class BaseViewModel : ExtendedViewModel
    {
        private string _pageTitle = string.Empty;
        private AvatarImageSource _avatar;
        private LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>();
        private LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();
        private LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

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

        protected void HandleException(Exception ex, string message = null)
        {
            if (ex is ApiException apiException && apiException.Error != null)
            {
                message = apiException.Error.GetSingleMessage();
            }

            Xamarin.Essentials.MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await _deviceActionService.Value.HideLoadingAsync();
                await _platformUtilsService.Value.ShowDialogAsync(message ?? AppResources.GenericErrorMessage);
            }).FireAndForget();
            _logger.Value.Exception(ex);
        }

        protected ICommand CreateDefaultAsyncCommnad(Func<Task> execute)
        {
            return new AsyncCommand(execute,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }
    }
}
