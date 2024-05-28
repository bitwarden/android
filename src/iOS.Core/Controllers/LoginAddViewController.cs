using Bit.App.Models;
using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Models;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using Microsoft.Maui.Controls.Compatibility;
using UIKit;
using Microsoft.Maui.Platform;
using CollectionView = Bit.Core.Models.View.CollectionView;
using Bit.Core.Models.Domain;
using Bit.Core.Enums;

namespace Bit.iOS.Core.Controllers
{
    public abstract class LoginAddViewController : ExtendedUITableViewController
    {
        private ICipherService _cipherService;
        private IFolderService _folderService;
        private IStorageService _storageService;
        private IOrganizationService _organizationService;
        private ICollectionService _collectionService;
        private IPolicyService _policyService;
        private IEnumerable<FolderView> _folders;
        private IEnumerable<CollectionView> _collections;
        private IEnumerable<CollectionView> _writeableCollections;
        private IEnumerable<Organization> _organizations;

        private bool _personalOwnershipPolicyApplies;
        //Here we have the different sizes dependent of the _personalOwnershipPolicyApplies
        private readonly int[] _personalVault = { 3, 1, 2, 1 };
        private readonly int[] _groupVault = { 1, 3, 1, 2, 1, 2 };
        private ExtendedUITableViewCell[][] _tableViewCell;

        protected LoginAddViewController(IntPtr handle)
            : base(handle)
        { }

        public AppExtensionContext Context { get; set; }
        public FormEntryTableViewCell NameCell { get; set; } = new FormEntryTableViewCell(AppResources.Name);
        public FormEntryTableViewCell UsernameCell { get; set; } = new FormEntryTableViewCell(AppResources.Username, buttonsConfig: FormEntryTableViewCell.ButtonsConfig.One);
        public FormEntryTableViewCell PasswordCell { get; set; } = new FormEntryTableViewCell(AppResources.Password, buttonsConfig: FormEntryTableViewCell.ButtonsConfig.Two);
        public FormEntryTableViewCell UriCell { get; set; } = new FormEntryTableViewCell(AppResources.URI);
        public SwitchTableViewCell FavoriteCell { get; set; } = new SwitchTableViewCell(AppResources.Favorite);
        public FormEntryTableViewCell NotesCell { get; set; } = new FormEntryTableViewCell(
            useTextView: true, height: 180);
        public PickerTableViewCell FolderCell { get; set; } = new PickerTableViewCell(AppResources.Folder);
        public PickerTableViewCell OrganizationCell { get; set; } = new PickerTableViewCell(AppResources.Organization);
        public PickerTableViewCell CollectionCell { get; set; } = new PickerTableViewCell(AppResources.Collections);

        public FormEntryTableViewCell PersonalOwnershipPolicyCell { get; set; } = new FormEntryTableViewCell(empty: true);

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIBarButtonItem BaseSaveButton { get; }
        public abstract Action<string> Success { get; }

        protected bool IsCreatingPasskey { get; set; }

        public override void ViewDidLoad()
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");

            BaseNavItem.Title = AppResources.AddItem;
            BaseCancelButton.Title = AppResources.Cancel;
            BaseSaveButton.Title = AppResources.Save;
            View.BackgroundColor = ThemeHelpers.BackgroundColor;

            NameCell.TextField.Text = Context?.Uri?.Host ?? string.Empty;
            NameCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            NameCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                UsernameCell.TextField.BecomeFirstResponder();
                return true;
            };

            UsernameCell.TextField.AutocapitalizationType = UITextAutocapitalizationType.None;
            UsernameCell.TextField.AutocorrectionType = UITextAutocorrectionType.No;
            UsernameCell.TextField.SpellCheckingType = UITextSpellCheckingType.No;
            UsernameCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            UsernameCell.Button.TitleLabel.Font = UIFont.FromName("bwi-font", 28f);
            UsernameCell.Button.SetTitle(BitwardenIcons.Generate, UIControlState.Normal);
            UsernameCell.Button.AccessibilityLabel = AppResources.GenerateUsername;
            UsernameCell.Button.TouchUpInside += (sender, e) =>
            {
                LaunchUsernameGeneratorFlow();
            };
            UsernameCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                PasswordCell.TextField.BecomeFirstResponder();
                return true;
            };

            PasswordCell.TextField.SecureTextEntry = true;
            PasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            PasswordCell.Button.TitleLabel.Font = UIFont.FromName("bwi-font", 28f);
            PasswordCell.Button.SetTitle(BitwardenIcons.Generate, UIControlState.Normal);
            PasswordCell.Button.AccessibilityLabel = AppResources.GeneratePassword;
            PasswordCell.Button.TouchUpInside += (sender, e) =>
            {
                PerformSegue("passwordGeneratorSegue", this);
            };

            PasswordCell.ConfigureToggleSecureTextCell(true);
            PasswordCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                UriCell.TextField.BecomeFirstResponder();
                return true;
            };

            UriCell.TextField.Text = Context?.UrlString ?? string.Empty;
            UriCell.TextField.KeyboardType = UIKeyboardType.Url;
            UriCell.TextField.ReturnKeyType = UIReturnKeyType.Next;
            UriCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                NotesCell.TextView.BecomeFirstResponder();
                return true;
            };

            _folders = _folderService.GetAllDecryptedAsync().GetAwaiter().GetResult();
            var folderNames = _folders.Select(s => s.Name).OrderBy(s => s).ToList();
            folderNames.Insert(0, AppResources.FolderNone);
            FolderCell.Items = folderNames;

            _personalOwnershipPolicyApplies = _policyService.PolicyAppliesToUser(PolicyType.PersonalOwnership).GetAwaiter().GetResult();
            var index = 0;
            if (_personalOwnershipPolicyApplies)
            {

                _organizations = _organizationService.GetAllAsync().GetAwaiter().GetResult().OrderBy(o => o.Name).ToList();

                OrganizationCell.Items = _organizations.Select(o => o.Name).ToList();
                OrganizationCell.ValueChanged += Type_ValueChanged;

                _collections = _collectionService.GetAllDecryptedAsync().GetAwaiter().GetResult();
                _writeableCollections = _collections.Where(c => !c.ReadOnly).OrderBy(c => c.Name).ToList();
                var collectionsNames = _writeableCollections.Where(c => c.OrganizationId == _organizations.ElementAt(OrganizationCell.SelectedIndex).Id).Select(s => s.Name).ToList();
                if (collectionsNames.Count == 0)
                {
                    collectionsNames.Insert(0, AppResources.NoCollectionsToList);
                }

                CollectionCell.Items = collectionsNames;

                _tableViewCell = new ExtendedUITableViewCell[_groupVault.Length][];
                _tableViewCell[0] = new ExtendedUITableViewCell[] { PersonalOwnershipPolicyCell };
                _tableViewCell[5] = new ExtendedUITableViewCell[] { OrganizationCell, CollectionCell };

                index = 1;
            }
            else
            {
                _tableViewCell = new ExtendedUITableViewCell[_personalVault.Length][];
            }

            _tableViewCell[index] = new ExtendedUITableViewCell[] { NameCell, UsernameCell, PasswordCell };
            _tableViewCell[++index] = new ExtendedUITableViewCell[] { UriCell };
            _tableViewCell[++index] = new ExtendedUITableViewCell[] { FolderCell, FavoriteCell };
            _tableViewCell[++index] = new ExtendedUITableViewCell[] { NotesCell };

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 70;
            TableView.Source = new TableSource(this);
            TableView.AllowsSelection = true;

            base.ViewDidLoad();
        }

        private void Type_ValueChanged(object sender, EventArgs e)
        {
            var organizationList = _organizations.OrderBy(o => o.Name).ToList();
            var collectionsNames = _writeableCollections.Where(c => c.OrganizationId == _organizations.ElementAt(OrganizationCell.SelectedIndex).Id).Select(s => s.Name).ToList();
            if (collectionsNames.Count == 0)
            {
                collectionsNames.Insert(0, AppResources.NoCollectionsToList);
            }

            CollectionCell.Items = collectionsNames;
            _tableViewCell[5][1].ReloadInputViews();
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
        }

        protected virtual async Task SaveAsync()
        {
            try
            {
                /*
                if (!_connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }
                */

                if (!IsCreatingPasskey && string.IsNullOrWhiteSpace(PasswordCell?.TextField?.Text))
                {
                    DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                        AppResources.Password), AppResources.Ok);
                    return;
                }

                if (string.IsNullOrWhiteSpace(NameCell?.TextField?.Text))
                {
                    DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                        AppResources.Name), AppResources.Ok);
                    return;
                }

                var cipher = new CipherView
                {
                    Name = NameCell.TextField.Text,
                    Notes = string.IsNullOrWhiteSpace(NotesCell?.TextView?.Text) ? null : NotesCell.TextView.Text,
                    Favorite = FavoriteCell.Switch.On,
                    FolderId = FolderCell.SelectedIndex == 0 ?
                        null : _folders.ElementAtOrDefault(FolderCell.SelectedIndex - 1)?.Id,
                    Type = Bit.Core.Enums.CipherType.Login,
                    Login = new LoginView
                    {
                        Uris = null,
                        Username = string.IsNullOrWhiteSpace(UsernameCell?.TextField?.Text) ?
                            null : UsernameCell.TextField.Text,
                        Password = string.IsNullOrWhiteSpace(PasswordCell.TextField.Text) ?
                            null : PasswordCell.TextField.Text,
                    },
                    CollectionIds = CollectionCell.SelectedItem.ToString() == AppResources.NoCollectionsToList ?
                            null : new HashSet<string> { _collections.ElementAtOrDefault(CollectionCell.SelectedIndex)?.Id },
                    OrganizationId = OrganizationCell.SelectedItem != null ? _organizations.ElementAtOrDefault(CollectionCell.SelectedIndex)?.Id : null,
                };

                if (!string.IsNullOrWhiteSpace(UriCell?.TextField?.Text))
                {
                    cipher.Login.Uris = new List<LoginUriView>
                    {
                        new LoginUriView
                        {
                            Uri = UriCell.TextField.Text
                        }
                    };
                }

                await EncryptAndSaveAsync(cipher);
            }
            catch (ApiException e)
            {
                if (e?.Error != null)
                {
                    DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok);
            }
        }

        protected virtual async Task EncryptAndSaveAsync(CipherView cipher)
        {
            var loadingAlert = Dialogs.CreateLoadingAlert(AppResources.Saving);
            try
            {
                PresentViewController(loadingAlert, true, null);

                var cipherDomain = await _cipherService.EncryptAsync(cipher);
                await _cipherService.SaveWithServerAsync(cipherDomain);
                await loadingAlert.DismissViewControllerAsync(true);
                await _storageService.SaveAsync(Bit.Core.Constants.ClearCiphersCacheKey, true);
                if (await ASHelpers.IdentitiesSupportIncrementalAsync())
                {
                    var identity = await ASHelpers.GetCipherPasswordIdentityAsync(cipherDomain.Id);
                    if (identity != null)
                    {
                        await ASCredentialIdentityStoreExtensions.SaveCredentialIdentitiesAsync(identity);
                    }
                }
                else
                {
                    await ASHelpers.ReplaceAllIdentitiesAsync();
                }
                Success(cipherDomain.Id);
            }
            catch
            {
                await loadingAlert.DismissViewControllerAsync(false);
                throw;
            }
        }

        public void DisplayAlert(string title, string message, string accept)
        {
            var alert = Dialogs.CreateAlert(title, message, accept);
            PresentViewController(alert, true, null);
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle,
                AppResources.InternetConnectionRequiredMessage, AppResources.Ok);
        }

        private void LaunchUsernameGeneratorFlow()
        {
            var appOptions = new AppOptions { IosExtension = true };
            var app = new App.App(appOptions);

            var generatorPage = new GeneratorPage(false, selectAction: (username) =>
            {
                UsernameCell.TextField.Text = username;
                DismissViewController(false, null);
            }, isUsernameGenerator: true, emailWebsite: NameCell.TextField.Text, appOptions: appOptions);

            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(generatorPage);

            var navigationPage = new NavigationPage(generatorPage);
            var generatorController = navigationPage.ToUIViewController(MauiContextSingleton.Instance.MauiContext);
            generatorController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(generatorController, true, null);
        }

        public class TableSource : ExtendedUITableViewSource
        {
            private LoginAddViewController _controller;

            public TableSource(LoginAddViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                return _controller._tableViewCell[indexPath.Section][indexPath.Row];
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint NumberOfSections(UITableView tableView)
            {
                if (_controller._personalOwnershipPolicyApplies)
                {
                    return _controller._groupVault.Length;
                }
                else
                {
                    return _controller._personalVault.Length;
                }
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if (_controller._personalOwnershipPolicyApplies)
                {
                    return _controller._groupVault[section];
                }
                else
                {
                    return _controller._personalVault[section];
                }
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                if (_controller._personalOwnershipPolicyApplies)
                {
                    return section == 0 || section == 1 || section == 4 || section == 5 ? UITableView.AutomaticDimension : 0.00001f;
                }
                else
                {
                    return section == 0 || section == 3 ? UITableView.AutomaticDimension : 0.00001f;
                }
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                if (_controller._personalOwnershipPolicyApplies)
                {
                    if (section == 0)
                    {
                        return AppResources.PersonalOwnershipPolicyInEffect;
                    }
                    if (section == 1)
                    {
                        return AppResources.ItemInformation;
                    }
                    else if (section == 4)
                    {
                        return AppResources.Notes;
                    }
                    else if (section == 5)
                    {
                        return AppResources.WhoOwnsThisItem;
                    }
                }
                else
                {
                    if (section == 0)
                    {
                        return AppResources.ItemInformation;
                    }
                    else if (section == 3)
                    {
                        return AppResources.Notes;
                    }
                }

                return string.Empty;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);

                var cell = tableView.CellAt(indexPath);
                if (cell == null)
                {
                    return;
                }

                var selectableCell = cell as ISelectable;
                if (selectableCell != null)
                {
                    selectableCell.Select();
                }
            }
        }
    }
}
