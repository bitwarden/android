using System.Threading.Tasks;
using System.Windows.Input;
using Bit.Core.Resources.Localization;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;

using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class VaultSettingsPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IEnvironmentService _environmentService;

        public VaultSettingsPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();

            GoToFoldersCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new FoldersPage())),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            GoToExportVaultCommand = CreateDefaultAsyncRelayCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new ExportVaultPage())),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            GoToImportItemsCommand = CreateDefaultAsyncRelayCommand(GoToImportItemsAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand GoToFoldersCommand { get; }
        public ICommand GoToExportVaultCommand { get; }
        public ICommand GoToImportItemsCommand { get; }

        private async Task GoToImportItemsAsync()
        {
            var webVaultUrl = _environmentService.GetWebVaultUrl();
            var body = string.Format(AppResources.YouCanImportDataToYourVaultOnX, webVaultUrl);
            if (await _platformUtilsService.ShowDialogAsync(body, AppResources.ContinueToWebApp, AppResources.Continue, AppResources.Cancel))
            {
                _platformUtilsService.LaunchUri(webVaultUrl);
            }
        }
    }
}
