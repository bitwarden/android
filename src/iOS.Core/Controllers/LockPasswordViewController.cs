using System;
using UIKit;
using Foundation;
using Bit.iOS.Core.Views;
using Bit.App.Resources;
using Bit.iOS.Core.Utilities;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Enums;

namespace Bit.iOS.Core.Controllers
{
    public abstract class LockPasswordViewController : ExtendedUITableViewController
    {
        private ILockService _lockService;
        private ICryptoService _cryptoService;
        private IDeviceActionService _deviceActionService;
        private IUserService _userService;
        private IStorageService _storageService;
        private IStorageService _secureStorageService;
        private IPlatformUtilsService _platformUtilsService;
        private Tuple<bool, bool> _pinSet;
        private bool _pinLock;
        private bool _fingerprintLock;
        private int _invalidPinAttempts;

        public LockPasswordViewController(IntPtr handle)
            : base(handle)
        { }

        public abstract UINavigationItem BaseNavItem { get; }
        public abstract UIBarButtonItem BaseCancelButton { get; }
        public abstract UIBarButtonItem BaseSubmitButton { get; }
        public abstract Action Success { get; }
        public abstract Action Cancel { get; }

        public FormEntryTableViewCell MasterPasswordCell { get; set; } = new FormEntryTableViewCell(
            AppResources.MasterPassword);

        public override void ViewDidLoad()
        {
            _lockService = ServiceContainer.Resolve<ILockService>("lockService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");

            _pinSet = _lockService.IsPinLockSetAsync().GetAwaiter().GetResult();
            _pinLock = (_pinSet.Item1 && _lockService.PinProtectedKey != null) || _pinSet.Item2;
            _fingerprintLock = _lockService.IsFingerprintLockSetAsync().GetAwaiter().GetResult();

            BaseNavItem.Title = _pinLock ? AppResources.VerifyPIN : AppResources.VerifyMasterPassword;
            BaseCancelButton.Title = AppResources.Cancel;
            BaseSubmitButton.Title = AppResources.Submit;

            var descriptor = UIFontDescriptor.PreferredBody;

            MasterPasswordCell.Label.Text = _pinLock ? AppResources.PIN : AppResources.MasterPassword;
            MasterPasswordCell.TextField.SecureTextEntry = true;
            MasterPasswordCell.TextField.ReturnKeyType = UIReturnKeyType.Go;
            MasterPasswordCell.TextField.ShouldReturn += (UITextField tf) =>
            {
                CheckPasswordAsync().GetAwaiter().GetResult();
                return true;
            };
            if (_pinLock)
            {
                MasterPasswordCell.TextField.KeyboardType = UIKeyboardType.NumberPad;
            }

            TableView.RowHeight = UITableView.AutomaticDimension;
            TableView.EstimatedRowHeight = 70;
            TableView.Source = new TableSource(this);
            TableView.AllowsSelection = true;

            base.ViewDidLoad();

            if (_fingerprintLock)
            {
                var tasks = Task.Run(async () =>
                {
                    await Task.Delay(500);
                    NSRunLoop.Main.BeginInvokeOnMainThread(async () => await PromptFingerprintAsync());
                });
            }
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            if (!_fingerprintLock)
            {
                MasterPasswordCell.TextField.BecomeFirstResponder();
            }
        }

        protected async Task CheckPasswordAsync()
        {
            if (string.IsNullOrWhiteSpace(MasterPasswordCell.TextField.Text))
            {
                var alert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired,
                        _pinLock ? AppResources.PIN : AppResources.MasterPassword),
                    AppResources.Ok);
                PresentViewController(alert, true, null);
                return;
            }

            var email = await _userService.GetEmailAsync();
            var kdf = await _userService.GetKdfAsync();
            var kdfIterations = await _userService.GetKdfIterationsAsync();
            var inputtedValue = MasterPasswordCell.TextField.Text;

            if (_pinLock)
            {
                var failed = true;
                try
                {
                    if (_pinSet.Item1)
                    {
                        var key = await _cryptoService.MakeKeyFromPinAsync(inputtedValue, email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000),
                            _lockService.PinProtectedKey);
                        var encKey = await _cryptoService.GetEncKeyAsync(key);
                        var protectedPin = await _storageService.GetAsync<string>(Bit.Core.Constants.ProtectedPin);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new CipherString(protectedPin), encKey);
                        failed = decPin != inputtedValue;
                        if (!failed)
                        {
                            await SetKeyAndContinueAsync(key);
                        }
                    }
                    else
                    {
                        var key2 = await _cryptoService.MakeKeyFromPinAsync(inputtedValue, email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
                        failed = false;
                        await SetKeyAndContinueAsync(key2);
                    }
                }
                catch
                {
                    failed = true;
                }
                if (failed)
                {
                    _invalidPinAttempts++;
                    if (_invalidPinAttempts >= 5)
                    {
                        Cancel?.Invoke();
                        return;
                    }
                    InvalidValue();
                }
            }
            else
            {
                var key2 = await _cryptoService.MakeKeyAsync(inputtedValue, email, kdf, kdfIterations);
                var keyHash = await _cryptoService.HashPasswordAsync(inputtedValue, key2);
                var storedKeyHash = await _cryptoService.GetKeyHashAsync();
                if (storedKeyHash == null)
                {
                    var oldKey = await _secureStorageService.GetAsync<string>("oldKey");
                    if (key2.KeyB64 == oldKey)
                    {
                        await _secureStorageService.RemoveAsync("oldKey");
                        await _cryptoService.SetKeyHashAsync(keyHash);
                        storedKeyHash = keyHash;
                    }
                }
                if (storedKeyHash != null && keyHash != null && storedKeyHash == keyHash)
                {
                    if (_pinSet.Item1)
                    {
                        var protectedPin = await _storageService.GetAsync<string>(Bit.Core.Constants.ProtectedPin);
                        var encKey = await _cryptoService.GetEncKeyAsync(key2);
                        var decPin = await _cryptoService.DecryptToUtf8Async(new CipherString(protectedPin), encKey);
                        var pinKey = await _cryptoService.MakePinKeyAysnc(decPin, email,
                            kdf.GetValueOrDefault(KdfType.PBKDF2_SHA256), kdfIterations.GetValueOrDefault(5000));
                        _lockService.PinProtectedKey = await _cryptoService.EncryptAsync(key2.Key, pinKey);
                    }
                    await SetKeyAndContinueAsync(key2);
                }
                else
                {
                    InvalidValue();
                }
            }
        }

        private async Task SetKeyAndContinueAsync(SymmetricCryptoKey key)
        {
            var hasKey = await _cryptoService.HasKeyAsync();
            if (!hasKey)
            {
                await _cryptoService.SetKeyAsync(key);
            }
            DoContinue();
        }

        private void DoContinue()
        {
            _lockService.FingerprintLocked = false;
            MasterPasswordCell.TextField.ResignFirstResponder();
            Success();
        }

        public async Task PromptFingerprintAsync()
        {
            if (!_fingerprintLock)
            {
                return;
            }
            var success = await _platformUtilsService.AuthenticateBiometricAsync(null,
                _pinLock ? AppResources.PIN : AppResources.MasterPassword,
                () => MasterPasswordCell.TextField.BecomeFirstResponder());
            _lockService.FingerprintLocked = !success;
            if (success)
            {
                DoContinue();
            }
        }

        private void InvalidValue()
        {
            var alert = Dialogs.CreateAlert(AppResources.AnErrorHasOccurred,
                string.Format(null, _pinLock ? AppResources.PIN : AppResources.InvalidMasterPassword),
                AppResources.Ok, (a) =>
                    {

                        MasterPasswordCell.TextField.Text = string.Empty;
                        MasterPasswordCell.TextField.BecomeFirstResponder();
                    });
            PresentViewController(alert, true, null);
        }

        public class TableSource : ExtendedUITableViewSource
        {
            private LockPasswordViewController _controller;

            public TableSource(LockPasswordViewController controller)
            {
                _controller = controller;
            }

            public override UITableViewCell GetCell(UITableView tableView, NSIndexPath indexPath)
            {
                if (indexPath.Section == 0)
                {
                    if (indexPath.Row == 0)
                    {
                        return _controller.MasterPasswordCell;
                    }
                }
                else if (indexPath.Section == 1)
                {
                    if (indexPath.Row == 0)
                    {
                        var fingerprintButtonText = _controller._deviceActionService.SupportsFaceBiometric() ?
                            AppResources.UseFaceIDToUnlock : AppResources.UseFingerprintToUnlock;
                        var cell = new ExtendedUITableViewCell();
                        cell.TextLabel.TextColor = ThemeHelpers.PrimaryColor;
                        cell.TextLabel.Text = fingerprintButtonText;
                        return cell;
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
                return _controller._fingerprintLock ? 2 : 1;
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
                tableView.DeselectRow(indexPath, true);
                tableView.EndEditing(true);
                if (indexPath.Section == 1 && indexPath.Row == 0)
                {
                    var task = _controller.PromptFingerprintAsync();
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
