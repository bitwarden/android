using System.Windows.Input;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class VaultSettingsPageViewModel : BaseViewModel
    {
        public VaultSettingsPageViewModel()
        {
            var platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();

            GoToFoldersCommand = new AsyncCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new FoldersPage())),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            GoToExportVaultCommand = new AsyncCommand(() => Page.Navigation.PushModalAsync(new NavigationPage(new ExportVaultPage())),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            GoToImportItemsCommand = new AsyncCommand(
                () => MainThread.InvokeOnMainThreadAsync(() => platformUtilsService.LaunchUri(ExternalLinksConstants.HELP_IMPORT_DATA)),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand GoToFoldersCommand { get; }
        public ICommand GoToExportVaultCommand { get; }
        public ICommand GoToImportItemsCommand { get; }
    }
}
