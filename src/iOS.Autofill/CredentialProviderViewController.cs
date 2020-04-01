using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Utilities;
using Foundation;
using System;
using System.Threading.Tasks;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController
    {
        private Context _context;
        private bool _initedAppCenter;

        public CredentialProviderViewController(IntPtr handle)
            : base(handle)
        {
            ModalPresentationStyle = UIModalPresentationStyle.FullScreen;
        }

        public override void ViewDidLoad()
        {
            InitApp();
            base.ViewDidLoad();
            Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
            View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;
            _context = new Context
            {
                ExtContext = ExtensionContext
            };
        }

        public override void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers)
        {
            InitAppIfNeeded();
            _context.ServiceIdentifiers = serviceIdentifiers;
            if (serviceIdentifiers.Length > 0)
            {
                var uri = serviceIdentifiers[0].Identifier;
                if (serviceIdentifiers[0].Type == ASCredentialServiceIdentifierType.Domain)
                {
                    uri = string.Concat("https://", uri);
                }
                _context.UrlString = uri;
            }
            if (!CheckAuthed())
            {
                return;
            }
            if (IsLocked())
            {
                PerformSegue("lockPasswordSegue", this);
            }
            else
            {
                if (_context.ServiceIdentifiers == null || _context.ServiceIdentifiers.Length == 0)
                {
                    PerformSegue("loginSearchSegue", this);
                }
                else
                {
                    PerformSegue("loginListSegue", this);
                }
            }
        }

        public override void ProvideCredentialWithoutUserInteraction(ASPasswordCredentialIdentity credentialIdentity)
        {
            InitAppIfNeeded();
            if (!IsAuthed() || IsLocked())
            {
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserInteractionRequired), null);
                ExtensionContext.CancelRequest(err);
                return;
            }
            _context.CredentialIdentity = credentialIdentity;
            ProvideCredentialAsync().GetAwaiter().GetResult();
        }

        public override void PrepareInterfaceToProvideCredential(ASPasswordCredentialIdentity credentialIdentity)
        {
            InitAppIfNeeded();
            if (!CheckAuthed())
            {
                return;
            }
            _context.CredentialIdentity = credentialIdentity;
            CheckLock(async () => await ProvideCredentialAsync());
        }

        public override void PrepareInterfaceForExtensionConfiguration()
        {
            InitAppIfNeeded();
            _context.Configuring = true;
            if (!CheckAuthed())
            {
                return;
            }
            CheckLock(() => PerformSegue("setupSegue", this));
        }

        public void CompleteRequest(string id = null, string username = null,
            string password = null, string totp = null)
        {
            if ((_context?.Configuring ?? true) && string.IsNullOrWhiteSpace(password))
            {
                ServiceContainer.Reset();
                ExtensionContext?.CompleteExtensionConfigurationRequest();
                return;
            }

            if (_context == null || string.IsNullOrWhiteSpace(username) || string.IsNullOrWhiteSpace(password))
            {
                ServiceContainer.Reset();
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserCanceled), null);
                NSRunLoop.Main.BeginInvokeOnMainThread(() => ExtensionContext?.CancelRequest(err));
                return;
            }

            if (!string.IsNullOrWhiteSpace(totp))
            {
                UIPasteboard.General.String = totp;
            }

            var cred = new ASPasswordCredential(username, password);
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                if (!string.IsNullOrWhiteSpace(id))
                {
                    var eventService = ServiceContainer.Resolve<IEventService>("eventService");
                    await eventService.CollectAsync(Bit.Core.Enums.EventType.Cipher_ClientAutofilled, id);
                }
                ServiceContainer.Reset();
                ExtensionContext?.CompleteRequest(cred, null);
            });
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LoginListViewController listLoginController)
                {
                    listLoginController.Context = _context;
                    listLoginController.CPViewController = this;
                }
                else if (navController.TopViewController is LoginSearchViewController listSearchController)
                {
                    listSearchController.Context = _context;
                    listSearchController.CPViewController = this;
                }
                else if (navController.TopViewController is LockPasswordViewController passwordViewController)
                {
                    passwordViewController.CPViewController = this;
                }
                else if (navController.TopViewController is SetupViewController setupViewController)
                {
                    setupViewController.CPViewController = this;
                }
            }
        }

        public void DismissLockAndContinue()
        {
            DismissViewController(false, async () =>
            {
                if (_context.CredentialIdentity != null)
                {
                    await ProvideCredentialAsync();
                    return;
                }
                if (_context.Configuring)
                {
                    PerformSegue("setupSegue", this);
                    return;
                }
                if (_context.ServiceIdentifiers == null || _context.ServiceIdentifiers.Length == 0)
                {
                    PerformSegue("loginSearchSegue", this);
                }
                else
                {
                    PerformSegue("loginListSegue", this);
                }
            });
        }

        private async Task ProvideCredentialAsync()
        {
            var cipherService = ServiceContainer.Resolve<ICipherService>("cipherService", true);
            var cipher = await cipherService?.GetAsync(_context.CredentialIdentity.RecordIdentifier);
            if (cipher == null || cipher.Type != Bit.Core.Enums.CipherType.Login)
            {
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.CredentialIdentityNotFound), null);
                ExtensionContext.CancelRequest(err);
                return;
            }

            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var decCipher = await cipher.DecryptAsync();
            string totpCode = null;
            var disableTotpCopy = await storageService.GetAsync<bool?>(Bit.Core.Constants.DisableAutoTotpCopyKey);
            if (!disableTotpCopy.GetValueOrDefault(false))
            {
                var userService = ServiceContainer.Resolve<IUserService>("userService");
                var canAccessPremiumAsync = await userService.CanAccessPremiumAsync();
                if (!string.IsNullOrWhiteSpace(decCipher.Login.Totp) &&
                    (canAccessPremiumAsync || cipher.OrganizationUseTotp))
                {
                    var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                    totpCode = await totpService.GetCodeAsync(decCipher.Login.Totp);
                }
            }

            CompleteRequest(decCipher.Id, decCipher.Login.Username, decCipher.Login.Password, totpCode);
        }

        private void CheckLock(Action notLockedAction)
        {
            if (IsLocked())
            {
                PerformSegue("lockPasswordSegue", this);
            }
            else
            {
                notLockedAction();
            }
        }

        private bool CheckAuthed()
        {
            if (!IsAuthed())
            {
                var alert = Dialogs.CreateAlert(null, AppResources.MustLogInMainAppAutofill, AppResources.Ok, (a) =>
                {
                    CompleteRequest();
                });
                PresentViewController(alert, true, null);
                return false;
            }
            return true;
        }

        private bool IsLocked()
        {
            var lockService = ServiceContainer.Resolve<ILockService>("lockService");
            return lockService.IsLockedAsync().GetAwaiter().GetResult();
        }

        private bool IsAuthed()
        {
            var userService = ServiceContainer.Resolve<IUserService>("userService");
            return userService.IsAuthenticatedAsync().GetAwaiter().GetResult();
        }

        private void InitApp()
        {
            if (ServiceContainer.RegisteredServices.Count > 0)
            {
                ServiceContainer.Reset();
            }
            iOSCoreHelpers.RegisterLocalServices();
            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            ServiceContainer.Init(deviceActionService.DeviceUserAgent);
            if (!_initedAppCenter)
            {
                iOSCoreHelpers.RegisterAppCenter();
                _initedAppCenter = true;
            }
            iOSCoreHelpers.Bootstrap();
            iOSCoreHelpers.AppearanceAdjustments(deviceActionService);
        }

        private void InitAppIfNeeded()
        {
            if (ServiceContainer.RegisteredServices == null || ServiceContainer.RegisteredServices.Count == 0)
            {
                InitApp();
            }
        }
    }
}
