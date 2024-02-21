using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill.Utilities
{
    public abstract class BaseLoginListTableSource<T> : ExtensionTableSource
        where T : UIViewController, ILoginListViewController
    {
        private IPasswordRepromptService _passwordRepromptService;
        private readonly LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();

        public BaseLoginListTableSource(T controller)
            : base(controller.Context, controller)
        {
            _controller = controller;
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>();
        }

        protected Context Context => (Context)_context;
        protected T Controller => (T)_controller;

        protected abstract string LoginAddSegue { get; }

        public async override void RowSelected(UITableView tableView, NSIndexPath indexPath)
        {
            try
            {
                if (Context.IsCreatingPasskey)
                {
                    await SelectRowForPasskeyCreationAsync(tableView, indexPath);
                    return;
                }

                await AutofillHelpers.TableRowSelectedAsync(tableView, indexPath, this,
                    Controller.CPViewController, Controller, _passwordRepromptService, LoginAddSegue);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private async Task SelectRowForPasskeyCreationAsync(UITableView tableView, NSIndexPath indexPath)
        {
            tableView.DeselectRow(indexPath, true);
            tableView.EndEditing(true);

            var item = Items.ElementAt(indexPath.Row);
            if (item is null)
            {
                await _platformUtilsService.Value.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred);
                return;
            }

            if (item.CipherView.Login.HasFido2Credentials
                &&
                !await _platformUtilsService.Value.ShowDialogAsync(
                    AppResources.ThisItemAlreadyContainsAPasskeyAreYouSureYouWantToOverwriteTheCurrentPasskey,
                    AppResources.OverwritePasskey,
                    AppResources.Yes,
                    AppResources.No))
            {
                return;
            }

            if (!await _passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(item.Reprompt))
            {
                return;
            }

            // TODO: Check user verification

            Context.ConfirmNewCredentialTcs.SetResult((item.Id, true));
        }
    }
}

