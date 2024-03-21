using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Pages;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;
using Microsoft.Maui.Controls.Compatibility;
using Microsoft.Maui.Platform;

namespace Bit.iOS.Core.Controllers
{
    public abstract class BaseLockPasswordViewController : ExtendedUIViewController
    {
        private IVaultTimeoutService _vaultTimeoutService;
        private ICryptoService _cryptoService;
        private IDeviceActionService _deviceActionService;
        private IStateService _stateService;
        private IStorageService _secureStorageService;
        private IPlatformUtilsService _platformUtilsService;
        private IBiometricService _biometricService;
        private IUserVerificationService _userVerificationService;
        private IAccountsManager _accountManager;
        private PinLockType _pinStatus;
        private bool _pinEnabled;
        private bool _biometricEnabled;
        private bool _biometricIntegrityValid = true;
        private bool _passwordReprompt = false;
        private bool _hasMasterPassword;
        private bool _biometricUnlockOnly = false;
        private bool _checkingPassword;

        protected bool autofillExtension = false;

        public BaseLockPasswordViewController()
        {
        }

        public BaseLockPasswordViewController(IntPtr handle)
            : base(handle)
        { }

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIBarButtonItem BaseSubmitButton { get; }
        public abstract Action Success { get; }
        public abstract Action Cancel { get; }
        public Action LaunchHomePage { get; set; }

        public FormEntryTableViewCell MasterPasswordCell { get; set; } = new FormEntryTableViewCell(
            AppResources.MasterPassword, buttonsConfig: FormEntryTableViewCell.ButtonsConfig.One);

        public string BiometricIntegritySourceKey { get; set; }

        public bool HasLoginOrUnlockMethod => _hasMasterPassword || _biometricEnabled || _pinEnabled;

        public UITableViewCell BiometricCell
        {
            get
            {
                var cell = new UITableViewCell();
                cell.BackgroundColor = ThemeHelpers.BackgroundColor;
                if (_biometricIntegrityValid)
                {
                    var biometricButtonText = _deviceActionService.SupportsFaceBiometric() ?
                    AppResources.UseFaceIDToUnlock : AppResources.UseFingerprintToUnlock;
                    cell.TextLabel.TextColor = ThemeHelpers.PrimaryColor;
                    cell.TextLabel.Text = biometricButtonText;
                }
                else
                {
                    cell.TextLabel.TextColor = ThemeHelpers.DangerColor;
                    cell.TextLabel.Font = ThemeHelpers.GetDangerFont();
                    cell.TextLabel.Lines = 0;
                    cell.TextLabel.LineBreakMode = UILineBreakMode.WordWrap;
                    cell.TextLabel.Text = AppResources.AccountBiometricInvalidatedExtension;
                }
                return cell;
            }
        }

        public abstract UITableView TableView { get; }

        public override async void ViewDidLoad()
        {
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _biometricService = ServiceContainer.Resolve<IBiometricService>("biometricService");
            _userVerificationService = ServiceContainer.Resolve<IUserVerificationService>();
            _accountManager = ServiceContainer.Resolve<IAccountsManager>("accountsManager");

            // We re-use the lock screen for autofill extension to verify master password
            // when trying to access protected items.
            if (autofillExtension && await _stateService.GetPasswordRepromptAutofillAsync())
            {
                _passwordReprompt = true;
                _pinStatus = PinLockType.Disabled;
                _pinEnabled = false;
                _biometricEnabled = false;
            }
            else
            {
                _pinStatus = await _vaultTimeoutService.GetPinLockTypeAsync();

                var ephemeralPinSet = await _stateService.GetPinKeyEncryptedUserKeyEphemeralAsync()
                    ?? await _stateService.GetPinProtectedKeyAsync();
                _pinEnabled = (_pinStatus == PinLockType.Transient && ephemeralPinSet != null) ||
                    _pinStatus == PinLockType.Persistent;

                _biometricEnabled = await IsBiometricsEnabledAsync();
                _hasMasterPassword = await _userVerificationService.HasMasterPasswordAsync();
                _biometricUnlockOnly = !_hasMasterPassword && _biometricEnabled && !_pinEnabled;

                if (_biometricUnlockOnly)
                {
                    await EnableBiometricsIfNeeded();
                }
                _biometricIntegrityValid = await _platformUtilsService.IsBiometricIntegrityValidAsync(BiometricIntegritySourceKey);
            }

            if (!HasLoginOrUnlockMethod)
            {
                // user doesn't have a login method
                // needs to go to homepage and login again
                LaunchHomePage?.Invoke();
                return;
            }

            if (_pinEnabled)
            {
                BaseNavItem.Title = AppResources.VerifyPIN;
            }
            else if (!_hasMasterPassword)
            {
                BaseNavItem.Title = AppResources.UnlockVault;
            }
            else
            {
                BaseNavItem.Title = AppResources.VerifyMasterPassword;
            }

            BaseCancelButton.Title = AppResources.Cancel;

            if (_biometricUnlockOnly)
            {
                BaseSubmitButton.Title = null;
                BaseSubmitButton.Enabled = false;
            }
            else
            {
                BaseSubmitButton.Title = AppResources.Submit;
            }

            var descriptor = UIFontDescriptor.PreferredBody;

            if (!_biometricUnlockOnly)
            {
                MasterPasswordCell.Label.Text = _pinEnabled ? AppResources.PIN : AppResources.MasterPassword;
                MasterPasswordCell.TextField.SecureTextEntry = true;
                MasterPasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Go;
                MasterPasswordCell.TextField.ShouldReturn += (UITextField tf) =>
                {
                    CheckPasswordAsync().FireAndForget();
                    return true;
                };
                if (_pinEnabled)
                {
                    MasterPasswordCell.TextField.KeyboardType = UIKeyboardType.NumberPad;
                }
                MasterPasswordCell.ConfigureToggleSecureTextCell();
            }

            if (TableView != null)
            {
                TableView.BackgroundColor = ThemeHelpers.BackgroundColor;
                TableView.SeparatorColor = ThemeHelpers.SeparatorColor;
                TableView.RowHeight = UITableView.AutomaticDimension;
                TableView.EstimatedRowHeight = 70;
                TableView.Source = new TableSource(this);
                TableView.AllowsSelection = true;
            }

            base.ViewDidLoad();

            if (_biometricEnabled)
            {
                if (!_biometricIntegrityValid)
                {
                    return;
                }
                var tasks = Task.Run(async () =>
                {
                    await Task.Delay(500);
                    NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
                    {
                        try
                        {
                            await PromptBiometricAsync();
                        }
                        catch (Exception ex)
                        {
                            LoggerHelper.LogEvenIfCantBeResolved(ex);
                            throw;
                        }
                    });
                });
            }
        }

        public override void ViewDidAppear(bool animated)
        {
            try
            {
                base.ViewDidAppear(animated);

                // Users with key connector and without biometric or pin has no MP to unlock with
                if (!_hasMasterPassword)
                {
                    if (!(_pinEnabled || _biometricEnabled) ||
                        (_biometricEnabled && !_biometricIntegrityValid))
                    {
                        PromptSSO();
                    }
                }
                else if (!_biometricEnabled || !_biometricIntegrityValid)
                {
                    MasterPasswordCell.TextField.BecomeFirstResponder();
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        protected async Task CheckPasswordAsync()
        {
            if (_checkingPassword)
            {
                return;
            }
            _checkingPassword = true;

            try
            {
                if (string.IsNullOrWhiteSpace(MasterPasswordCell.TextField.Text))
                {
                    var alert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                        string.Format(AppResources.ValidationFieldRequired,
                            _pinEnabled ? AppResources.PIN : AppResources.MasterPassword),
                        AppResources.Ok);
                    PresentViewController(alert, true, null);
                    return;
                }

                var email = await _stateService.GetEmailAsync();
                var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
                var inputtedValue = MasterPasswordCell.TextField.Text;

                // HACK: iOS extensions have constrained memory, given how it works Argon2Id, it's likely to crash
                // the extension depending on the argon2id memory configured.
                // So, we warn the user and advise to decrease the configured memory letting them the option to continue, if wanted.
                if (kdfConfig.Type == KdfType.Argon2id
                    &&
                    kdfConfig.Memory > Constants.MaximumArgon2IdMemoryBeforeExtensionCrashing
                    &&
                    !await _platformUtilsService.ShowDialogAsync(
                        AppResources.UnlockingMayFailDueToInsufficientMemoryDecreaseYourKDFMemorySettingsToResolve,
                        AppResources.Warning, AppResources.Continue, AppResources.Cancel))
                {
                    return;
                }

                if (_pinEnabled)
                {
                    await UnlockWithPinAsync(inputtedValue, email, kdfConfig);
                }
                else
                {
                    await UnlockWithMasterPasswordAsync(inputtedValue, email, kdfConfig);
                }
            }
            catch (LegacyUserException)
            {
                await HandleLegacyUserAsync();
            }
            finally
            {
                _checkingPassword = false;
            }
        }

        private async Task HandleFailedCredentialsAsync()
        {
            var invalidUnlockAttempts = await AppHelpers.IncrementInvalidUnlockAttemptsAsync();
            if (invalidUnlockAttempts >= 5)
            {
                await _accountManager.LogOutAsync(await _stateService.GetActiveUserIdAsync(), false, false);
                return;
            }
            InvalidValue();
        }

        private async Task UnlockWithPinAsync(string inputPin, string email, KdfConfig kdfConfig)
        {
            var failed = true;
            try
            {
                EncString userKeyPin = null;
                EncString oldPinProtected = null;
                if (_pinStatus == PinLockType.Persistent)
                {
                    userKeyPin = await _stateService.GetPinKeyEncryptedUserKeyAsync();
                    var oldEncryptedKey = await _stateService.GetPinProtectedAsync();
                    oldPinProtected = oldEncryptedKey != null ? new EncString(oldEncryptedKey) : null;
                }
                else if (_pinStatus == PinLockType.Transient)
                {
                    userKeyPin = await _stateService.GetPinKeyEncryptedUserKeyEphemeralAsync();
                    oldPinProtected = await _stateService.GetPinProtectedKeyAsync();
                }

                UserKey userKey;
                if (oldPinProtected != null)
                {
                    userKey = await _cryptoService.DecryptAndMigrateOldPinKeyAsync(
                        _pinStatus == PinLockType.Transient,
                        inputPin,
                        email,
                        kdfConfig,
                        oldPinProtected
                    );
                }
                else
                {
                    userKey = await _cryptoService.DecryptUserKeyWithPinAsync(
                        inputPin,
                        email,
                        kdfConfig,
                        userKeyPin
                    );
                }

                var protectedPin = await _stateService.GetProtectedPinAsync();
                var decryptedPin = await _cryptoService.DecryptToUtf8Async(new EncString(protectedPin), userKey);
                failed = decryptedPin != inputPin;
                if (!failed)
                {
                    await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                    await SetKeyAndContinueAsync(userKey);
                }
            }
            catch
            {
                failed = true;
            }

            if (failed)
            {
                await HandleFailedCredentialsAsync();
            }
        }

        private async Task UnlockWithMasterPasswordAsync(string inputPassword, string email, KdfConfig kdfConfig)
        {
            var masterKey = await _cryptoService.MakeMasterKeyAsync(inputPassword, email, kdfConfig);
            if (await _cryptoService.IsLegacyUserAsync(masterKey))
            {
                throw new LegacyUserException();
            }

            var storedPasswordHash = await _cryptoService.GetMasterKeyHashAsync();
            if (storedPasswordHash == null)
            {
                var oldKey = await _secureStorageService.GetAsync<string>("oldKey");
                if (masterKey.KeyB64 == oldKey)
                {
                    var localPasswordHash =
                        await _cryptoService.HashMasterKeyAsync(inputPassword, masterKey,
                            HashPurpose.LocalAuthorization);
                    await _secureStorageService.RemoveAsync("oldKey");
                    await _cryptoService.SetMasterKeyHashAsync(localPasswordHash);
                }
            }

            var passwordValid = await _cryptoService.CompareAndUpdateKeyHashAsync(inputPassword, masterKey);
            if (passwordValid)
            {
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                var userKey = await _cryptoService.DecryptUserKeyWithMasterKeyAsync(masterKey);
                await _cryptoService.SetMasterKeyAsync(masterKey);
                await SetKeyAndContinueAsync(userKey, true);
            }
            else
            {
                await HandleFailedCredentialsAsync();
            }
        }

        public async Task PromptBiometricAsync()
        {
            try
            {
                if (!_biometricEnabled || !_biometricIntegrityValid)
                {
                    return;
                }

                var success = await _platformUtilsService.AuthenticateBiometricAsync(null,
                    _pinEnabled ? AppResources.PIN : AppResources.MasterPassword,
                    () => MasterPasswordCell.TextField.BecomeFirstResponder(),
                    !_pinEnabled && !_hasMasterPassword) ?? false;

                await _stateService.SetBiometricLockedAsync(!success);
                if (success)
                {
                    var userKey = await _cryptoService.GetBiometricUnlockKeyAsync();
                    await SetKeyAndContinueAsync(userKey);
                }
            }
            catch (LegacyUserException)
            {
                await HandleLegacyUserAsync();
            }
        }

        public void PromptSSO()
        {
            var appOptions = new AppOptions { IosExtension = true };
            var loginPage = new LoginSsoPage(appOptions);
            var app = new App.App(appOptions);
            ThemeManager.SetTheme(app.Resources);
            ThemeManager.ApplyResourcesTo(loginPage);
            if (loginPage.BindingContext is LoginSsoPageViewModel vm)
            {
                vm.SsoAuthSuccessAction = () => DoContinue();
                vm.CloseAction = Cancel;
            }

            var navigationPage = new NavigationPage(loginPage);
            var loginController = navigationPage.ToUIViewController(MauiContextSingleton.Instance.MauiContext);
            loginController.ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
            PresentViewController(loginController, true, null);
        }

        private async Task SetKeyAndContinueAsync(UserKey userKey, bool masterPassword = false)
        {
            var hasKey = await _cryptoService.HasUserKeyAsync();
            if (!hasKey)
            {
                await _cryptoService.SetUserKeyAsync(userKey);
            }
            DoContinue(masterPassword);
        }

        private async void DoContinue(bool masterPassword = false)
        {
            if (masterPassword)
            {
                await _stateService.SetPasswordVerifiedAutofillAsync(true);
            }
            await EnableBiometricsIfNeeded();
            await _stateService.SetBiometricLockedAsync(false);
            MasterPasswordCell.TextField.ResignFirstResponder();
            Success();
        }

        private async Task EnableBiometricsIfNeeded()
        {
            // Re-enable biometrics if initial use
            if (_biometricEnabled & !_biometricIntegrityValid)
            {
                await _biometricService.SetupBiometricAsync(BiometricIntegritySourceKey);
            }
        }
        
        private async Task<bool> IsBiometricsEnabledAsync()
        {
            try
            {
                return await _vaultTimeoutService.IsBiometricLockSetAsync() &&
                                   await _biometricService.CanUseBiometricsUnlockAsync();
            }
            catch (LegacyUserException)
            {
                await HandleLegacyUserAsync();
            }
            return false;
        }

        private async Task HandleLegacyUserAsync()
        {
            // Legacy users must migrate on web vault.
            await _platformUtilsService.ShowDialogAsync(AppResources.EncryptionKeyMigrationRequiredDescriptionLong,
                AppResources.AnErrorHasOccurred,
                AppResources.Ok);
            await _vaultTimeoutService.LogOutAsync();
        }

        private void InvalidValue()
        {
            var alert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                string.Format(null, _pinEnabled ? AppResources.PIN : AppResources.InvalidMasterPassword),
                AppResources.Ok, (a) =>
                {

                    MasterPasswordCell.TextField.Text = string.Empty;
                    MasterPasswordCell.TextField.BecomeFirstResponder();
                });
            PresentViewController(alert, true, null);
        }

        protected override void Dispose(bool disposing)
        {
            base.Dispose(disposing);

            MasterPasswordCell?.Dispose();
            MasterPasswordCell = null;

            TableView?.Dispose();
        }

        public class TableSource : ExtendedUITableViewSource
        {
            private readonly WeakReference<BaseLockPasswordViewController> _controller;

            public TableSource(BaseLockPasswordViewController controller)
            {
                _controller = new WeakReference<BaseLockPasswordViewController>(controller);
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if (!_controller.TryGetTarget(out var controller))
                {
                    return new ExtendedUITableViewCell();
                }

                if (indexPath.Section == 0)
                {
                    if (indexPath.Row == 0)
                    {
                        if (controller._biometricUnlockOnly)
                        {
                            return controller.BiometricCell;
                        }
                        else
                        {
                            return controller.MasterPasswordCell;
                        }
                    }
                }
                else if (indexPath.Section == 1)
                {
                    if (indexPath.Row == 0)
                    {
                        if (controller._passwordReprompt)
                        {
                            var cell = new ExtendedUITableViewCell();
                            cell.TextLabel.TextColor = ThemeHelpers.DangerColor;
                            cell.TextLabel.Font = ThemeHelpers.GetDangerFont();
                            cell.TextLabel.Lines = 0;
                            cell.TextLabel.LineBreakMode = UILineBreakMode.WordWrap;
                            cell.TextLabel.Text = AppResources.PasswordConfirmationDesc;
                            return cell;
                        }
                        else if (!controller._biometricUnlockOnly)
                        {
                            return controller.BiometricCell;
                        }
                    }
                }
                return new ExtendedUITableViewCell();
            }

            public override nfloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
            {
                return UITableView.AutomaticDimension;
            }

            public override nint NumberOfSections(UITableView tableView)
            {
                if (!_controller.TryGetTarget(out var controller))
                {
                    return 0;
                }

                return (!controller._biometricUnlockOnly && controller._biometricEnabled) ||
                    controller._passwordReprompt
                    ? 2
                    : 1;
            }

            public override nint RowsInSection(UITableView tableview, nint section)
            {
                if (section <= 1)
                {
                    return 1;
                }
                return 0;
            }

            public override nfloat GetHeightForHeader(UITableView tableView, nint section)
            {
                return section == 1 ? 0.00001f : UITableView.AutomaticDimension;
            }

            public override string TitleForHeader(UITableView tableView, nint section)
            {
                return null;
            }

            public override void RowSelected(UITableView tableView, NSIndexPath indexPath)
            {
                if (!_controller.TryGetTarget(out var controller))
                {
                    return;
                }

                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);
                if (indexPath.Row == 0 &&
                    ((controller._biometricUnlockOnly && indexPath.Section == 0) ||
                    indexPath.Section == 1))
                {
                    var task = controller.PromptBiometricAsync();
                    return;
                }
                var cell = tableView.CellAt(indexPath);
                if (cell == null)
                {
                    return;
                }
                if (cell is ISelectable selectableCell)
                {
                    selectableCell.Select();
                }
            }
        }
    }
}

