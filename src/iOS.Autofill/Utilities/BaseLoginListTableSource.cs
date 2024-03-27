using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.ListItems;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Models;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using CoreGraphics;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill.Utilities
{
    public abstract class BaseLoginListTableSource<T> : ExtensionTableSource
        where T : UIViewController, ILoginListViewController
    {
        private const string NoDataCellIdentifier = "NoDataCellIdentifier";

        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();

        List<string> _allowedFido2CipherIds = null;

        public BaseLoginListTableSource(T controller)
            : base(controller.Context, controller)
        {
            _controller = controller;
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>();
        }

        protected Context Context => (Context)_context;
        protected T Controller => (T)_controller;

        protected abstract string LoginAddSegue { get; }

        public bool IsEmpty => Items?.Any() != true;

        public override void RegisterTableViewCells(UITableView tableView)
        {
            base.RegisterTableViewCells(tableView);

            tableView.RegisterClassForCellReuse(typeof(ExtendedUITableViewCell), NoDataCellIdentifier);
        }

        protected override void OnItemsLoaded(string searchFilter, CancellationToken ct)
        {
            base.OnItemsLoaded(searchFilter, ct);

            if (Context.IsPreparingListForPasskey && _allowedFido2CipherIds != null)
            {
                LoadFido2Ciphers(ct);
            }

            Controller.OnItemsLoaded(searchFilter);
        }

        private void LoadFido2Ciphers(CancellationToken ct)
        {
            var fido2CiphersToInsert = new List<CipherViewModel>();
            foreach (var item in Items.Where(i => i?.CipherView?.HasFido2Credential == true))
            {
                ct.ThrowIfCancellationRequested();

                if (!_allowedFido2CipherIds.Any()
                    ||
                    _allowedFido2CipherIds.Contains(item.Id))
                {
                    fido2CiphersToInsert.Add(item.ToPasskeyListItemCipherViewModel());
                }
            }

            if (!fido2CiphersToInsert.Any())
            {
                return;
            }

            fido2CiphersToInsert.Reverse();

            foreach (var item in fido2CiphersToInsert)
            {
                ct.ThrowIfCancellationRequested();

                Items.Insert(0, item);
            }
        }

        protected override CipherViewModel CreateCipherViewModel(CipherView cipherView)
        {
            var vm = base.CreateCipherViewModel(cipherView);
            vm.ForceSectionIcon = Context.IsPreparingListForPasskey;
            return vm;
        }

        protected override bool ShouldUseMainIconAsPasskey(CipherViewModel item, NSIndexPath indexPath)
        {
            if (!item.HasFido2Credential)
            {
                return false;
            }

            return IsPasskeySection(indexPath.Section) || !item.ForceSectionIcon;
        }

        protected override string GetCipherCellSubtitle(CipherViewModel item, NSIndexPath indexPath)
        {
            if (!item.HasFido2Credential)
            {
                return base.GetCipherCellSubtitle(item, indexPath);
            }

            if (Context.IsPreparingListForPasskey && !IsPasskeySection(indexPath.Section))
            {
                return item.Username;
            }

            return item.CipherView?.GetMainFido2CredentialUsername() ?? item.Username;
        }

        public override UIView GetViewForHeader(UITableView tableView, nint section)
        {
            try
            {
                if (Context.IsCreatingOrPreparingListForPasskey
                    &&
                    tableView.DequeueReusableHeaderFooterView(LoginListViewController.HEADER_SECTION_IDENTIFIER) is HeaderItemView headerItemView)
                {
                    if (Context.IsCreatingPasskey)
                    {
                        headerItemView.SetHeaderText(AppResources.ChooseALoginToSaveThisPasskeyTo);
                    }
                    else
                    {
                        headerItemView.SetHeaderText(IsPasskeySection(section) ? AppResources.Passkeys : AppResources.Passwords);
                    }
                    return headerItemView;
                }

                return new UIView(CGRect.Empty);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return new UIView();
            }
        }

        public override nint NumberOfSections(UITableView tableView)
        {
            try
            {
                if (Context.IsPreparingListForPasskey)
                {
                    return 2;
                }

                return 1;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return 1;
            }
        }

        public override nint RowsInSection(UITableView tableview, nint section)
        {
            try
            {
                if (Context.IsCreatingPasskey)
                {
                    return Items?.Count() ?? 0;
                }

                if (Context.IsPreparingListForPasskey)
                {
                    var isPasskeySection = IsPasskeySection(section);
                    var count = GetNumberOfItems(isPasskeySection);

                    return count == 0 ? 1 : count;
                }

                return base.RowsInSection(tableview, section);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return 1;
            }
        }

        private int GetNumberOfItems(bool forFido2)
        {
            if (!Context.IsPreparingListForPasskey)
            {
                return Items?.Count() ?? 0;
            }

            return Items?.Count(i => i.IsFido2ListItem == forFido2) ?? 0;
        }

        public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
        {
            try
            {
                if (GetNumberOfItems(IsPasskeySection(indexPath.Section)) == 0)
                {
                    var noDataCell = tableView.DequeueReusableCell(NoDataCellIdentifier);

                    var text = AppResources.NoItemsToList;
                    if (UIDevice.CurrentDevice.CheckSystemVersion(14, 0))
                    {
                        var config = noDataCell.DefaultContentConfiguration;
                        config.Text = text;
                        config.TextProperties.Color = ThemeHelpers.TextColor;
                        config.TextProperties.Alignment = UIListContentTextAlignment.Center;
                        config.TextProperties.LineBreakMode = UILineBreakMode.WordWrap;
                        config.TextProperties.NumberOfLines = 0;
                        noDataCell.ContentConfiguration = config;
                    }
                    else
                    {
                        noDataCell.TextLabel.Text = text;
                        noDataCell.TextLabel.TextAlignment = UITextAlignment.Center;
                        noDataCell.TextLabel.LineBreakMode = UILineBreakMode.WordWrap;
                        noDataCell.TextLabel.Lines = 0;
                        noDataCell.TextLabel.TextColor = ThemeHelpers.TextColor;
                    }

                    return noDataCell;
                }

                var cell = tableView.DequeueReusableCell(CipherLoginCellIdentifier);
                if (cell is null)
                {
                    throw new InvalidOperationException($"The cell {CipherLoginCellIdentifier} has not been registered in the UITableView");
                }
                return cell;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return new ExtendedUITableViewCell(UITableViewCellStyle.Default, "NoDataCell");
            }
        }

        internal void ReloadWithAllowedFido2Credentials(List<string> allowedCipherIds)
        {
            _allowedFido2CipherIds = allowedCipherIds;
            Controller.ReloadItemsAsync().FireAndForget();
        }

        bool IsPasskeySection(nint section) => section == 0;

        public async override void RowSelected(UITableView tableView, NSIndexPath indexPath)
        {
            try
            {
                if (Context.IsCreatingPasskey)
                {
                    await SelectRowForPasskeyCreationAsync(tableView, indexPath);
                    return;
                }

                if (Items == null || Items.Count() == 0)
                {
                    Controller.PerformSegue(LoginAddSegue, this);
                    return;
                }

                var item = await DeselectRowAndGetItemAsync(tableView, indexPath);
                if (item is null)
                {
                    return;
                }

                if (Context.IsPreparingListForPasskey && item.IsFido2ListItem)
                {
                    Context.PickCredentialForFido2GetAssertionFromListTcs.TrySetResult(item.Id);
                    return;
                }

                await AutofillHelpers.TableRowSelectedAsync(item, this,
                    Controller.CPViewController, Controller, _passwordRepromptService);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private async Task SelectRowForPasskeyCreationAsync(UITableView tableView, NSIndexPath indexPath)
        {
            var item = await DeselectRowAndGetItemAsync(tableView, indexPath);
            if (item is null)
            {
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

            Context.PickCredentialForFido2CreationTcs.TrySetResult((item.Id, null));
        }

        private async Task<CipherViewModel> DeselectRowAndGetItemAsync(UITableView tableView, NSIndexPath indexPath)
        {
            tableView.DeselectRow(indexPath, true);
            tableView.EndEditing(true);

            var item = Items.ElementAtOrDefault(GetIndexForItemAt(tableView, indexPath));
            if (item is null)
            {
                await _platformUtilsService.Value.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred);
                return null;
            }

            return item;
        }

        protected override int GetIndexForItemAt(UITableView tableView, NSIndexPath indexPath)
        {
            var index = indexPath.Row;
            if (indexPath.Section == 1)
            {
                index += (int)RowsInSection(tableView, 0);
            }
            return index;
        }
    }
}

