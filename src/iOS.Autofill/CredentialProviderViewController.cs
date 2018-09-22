using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.App.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core;
using Bit.iOS.Core.Services;
using Bit.iOS.Core.Utilities;
using Foundation;
using Plugin.Connectivity;
using Plugin.Fingerprint;
using Plugin.Settings.Abstractions;
using SimpleInjector;
using System;
using UIKit;
using XLabs.Ioc;
using XLabs.Ioc.SimpleInjectorContainer;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController
    {
        private Context _context;
        private bool _setupHockeyApp = false;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private ISettings _settings;

        public CredentialProviderViewController(IntPtr handle) : base(handle)
        { }

        public override void ViewDidLoad()
        {
            SetIoc();
            SetCulture();
            base.ViewDidLoad();
            _context = new Context();
            _context.ExtContext = ExtensionContext;
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _settings = Resolver.Resolve<ISettings>();

            if(!_setupHockeyApp)
            {
                var appIdService = Resolver.Resolve<IAppIdService>();
                var crashManagerDelegate = new HockeyAppCrashManagerDelegate(appIdService, Resolver.Resolve<IAuthService>());
                var manager = HockeyApp.iOS.BITHockeyManager.SharedHockeyManager;
                manager.Configure("51f96ae568ba45f699a18ad9f63046c3", crashManagerDelegate);
                manager.CrashManager.CrashManagerStatus = HockeyApp.iOS.BITCrashManagerStatus.AutoSend;
                manager.UserId = appIdService.AppId;
                manager.StartManager();
                manager.Authenticator.AuthenticateInstallation();
                _setupHockeyApp = true;
            }
        }

        public override void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers)
        {
            _context.ServiceIdentifiers = serviceIdentifiers;
            if(serviceIdentifiers.Length > 0)
            {
                var uri = serviceIdentifiers[0].Identifier;
                if(serviceIdentifiers[0].Type == ASCredentialServiceIdentifierType.Domain)
                {
                    uri = string.Concat("https://", uri);
                }
                _context.UrlString = uri;
            }
            if(!CheckAuthed())
            {
                return;
            }

            var lockService = Resolver.Resolve<ILockService>();
            var lockType = lockService.GetLockTypeAsync(false).GetAwaiter().GetResult();
            switch(lockType)
            {
                case App.Enums.LockType.Fingerprint:
                    PerformSegue("lockFingerprintSegue", this);
                    break;
                case App.Enums.LockType.PIN:
                    PerformSegue("lockPinSegue", this);
                    break;
                case App.Enums.LockType.Password:
                    PerformSegue("lockPasswordSegue", this);
                    break;
                default:
                    PerformSegue("loginListSegue", this);
                    break;
            }
        }

        public override void ProvideCredentialWithoutUserInteraction(ASPasswordCredentialIdentity credentialIdentity)
        {
            bool canGetCredentials = false;
            var authService = Resolver.Resolve<IAuthService>();
            if(authService.IsAuthenticated)
            {
                var lockService = Resolver.Resolve<ILockService>();
                var lockType = lockService.GetLockTypeAsync(false).GetAwaiter().GetResult();
                canGetCredentials = lockType == App.Enums.LockType.None;
            }

            if(!canGetCredentials)
            {
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserInteractionRequired), null);
                ExtensionContext.CancelRequest(err);
                return;
            }
            _context.CredentialIdentity = credentialIdentity;
            ProvideCredential();
        }

        public override void PrepareInterfaceToProvideCredential(ASPasswordCredentialIdentity credentialIdentity)
        {
            if(!CheckAuthed())
            {
                return;
            }
            _context.CredentialIdentity = credentialIdentity;
            CheckLock(() => ProvideCredential());
        }

        public override void PrepareInterfaceForExtensionConfiguration()
        {
            _context.Configuring = true;
            if(!CheckAuthed())
            {
                return;
            }
            CheckLock(() => PerformSegue("setupSegue", this));
        }

        public void CompleteRequest(string username = null, string password = null, string totp = null)
        {
            if(_context.Configuring)
            {
                ExtensionContext.CompleteExtensionConfigurationRequest();
                return;
            }

            if(string.IsNullOrWhiteSpace(username) && string.IsNullOrWhiteSpace(password))
            {
                _googleAnalyticsService.TrackAutofillExtensionEvent("Canceled");
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.UserCanceled), null);
                _googleAnalyticsService.Dispatch(() =>
                {
                    NSRunLoop.Main.BeginInvokeOnMainThread(() =>
                    {
                        ExtensionContext.CancelRequest(err);
                    });
                });
                return;
            }

            if(!string.IsNullOrWhiteSpace(totp))
            {
                UIPasteboard.General.String = totp;
            }

            _googleAnalyticsService.TrackAutofillExtensionEvent("AutoFilled");
            var cred = new ASPasswordCredential(username, password);
            _googleAnalyticsService.Dispatch(() =>
            {
                NSRunLoop.Main.BeginInvokeOnMainThread(() =>
                {
                    ExtensionContext.CompleteRequest(cred, null);
                });
            });
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            var navController = segue.DestinationViewController as UINavigationController;
            if(navController != null)
            {
                var listLoginController = navController.TopViewController as LoginListViewController;
                var fingerprintViewController = navController.TopViewController as LockFingerprintViewController;
                var pinViewController = navController.TopViewController as LockPinViewController;
                var passwordViewController = navController.TopViewController as LockPasswordViewController;
                var setupViewController = navController.TopViewController as SetupViewController;

                if(listLoginController != null)
                {
                    listLoginController.Context = _context;
                    listLoginController.CPViewController = this;
                }
                else if(fingerprintViewController != null)
                {
                    fingerprintViewController.CPViewController = this;
                }
                else if(pinViewController != null)
                {
                    pinViewController.CPViewController = this;
                }
                else if(passwordViewController != null)
                {
                    passwordViewController.CPViewController = this;
                }
                else if(setupViewController != null)
                {
                    setupViewController.CPViewController = this;
                }
            }
        }

        public void DismissLockAndContinue()
        {
            DismissViewController(false, () =>
            {
                if(_context.CredentialIdentity != null)
                {
                    ProvideCredential();
                    return;
                }
                if(_context.Configuring)
                {
                    PerformSegue("setupSegue", this);
                    return;
                }
                PerformSegue("loginListSegue", this);
            });
        }

        private void ProvideCredential()
        {
            var cipherService = Resolver.Resolve<ICipherService>();
            var cipher = cipherService.GetByIdAsync(_context.CredentialIdentity.RecordIdentifier).GetAwaiter().GetResult();
            if(cipher == null || cipher.Type != App.Enums.CipherType.Login)
            {
                var err = new NSError(new NSString("ASExtensionErrorDomain"),
                    Convert.ToInt32(ASExtensionErrorCode.CredentialIdentityNotFound), null);
                ExtensionContext.CancelRequest(err);
                return;
            }

            string totpCode = null;
            if(!_settings.GetValueOrDefault(App.Constants.SettingDisableTotpCopy, false))
            {
                var totpKey = cipher.Login.Totp?.Decrypt(cipher.OrganizationId);
                if(!string.IsNullOrWhiteSpace(totpKey))
                {
                    totpCode = Crypto.Totp(totpKey);
                }
            }

            CompleteRequest(cipher.Login.Username?.Decrypt(cipher.OrganizationId),
                cipher.Login.Password?.Decrypt(cipher.OrganizationId), totpCode);
        }

        private void CheckLock(Action notLockedAction)
        {
            var lockService = Resolver.Resolve<ILockService>();
            var lockType = lockService.GetLockTypeAsync(false).GetAwaiter().GetResult();
            switch(lockType)
            {
                case App.Enums.LockType.Fingerprint:
                    PerformSegue("lockFingerprintSegue", this);
                    break;
                case App.Enums.LockType.PIN:
                    PerformSegue("lockPinSegue", this);
                    break;
                case App.Enums.LockType.Password:
                    PerformSegue("lockPasswordSegue", this);
                    break;
                default:
                    notLockedAction();
                    break;
            }
        }

        private bool CheckAuthed()
        {
            var authService = Resolver.Resolve<IAuthService>();
            if(!authService.IsAuthenticated)
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

        private void SetIoc()
        {
            var container = new Container();

            // Services
            container.RegisterSingleton<IDatabaseService, DatabaseService>();
            container.RegisterSingleton<ISqlService, SqlService>();
            container.RegisterSingleton<ISecureStorageService, KeyChainStorageService>();
            container.RegisterSingleton<ICryptoService, CryptoService>();
            container.RegisterSingleton<IKeyDerivationService, CommonCryptoKeyDerivationService>();
            container.RegisterSingleton<IAuthService, AuthService>();
            container.RegisterSingleton<IFolderService, FolderService>();
            container.RegisterSingleton<ICollectionService, CollectionService>();
            container.RegisterSingleton<ICipherService, CipherService>();
            container.RegisterSingleton<ISyncService, SyncService>();
            container.RegisterSingleton<IDeviceActionService, NoopDeviceActionService>();
            container.RegisterSingleton<IAppIdService, AppIdService>();
            container.RegisterSingleton<IPasswordGenerationService, PasswordGenerationService>();
            container.RegisterSingleton<ILockService, LockService>();
            container.RegisterSingleton<IAppInfoService, AppInfoService>();
            container.RegisterSingleton<IGoogleAnalyticsService, GoogleAnalyticsService>();
            container.RegisterSingleton<IDeviceInfoService, DeviceInfoService>();
            container.RegisterSingleton<ILocalizeService, LocalizeService>();
            container.RegisterSingleton<ILogService, LogService>();
            container.RegisterSingleton<IHttpService, HttpService>();
            container.RegisterSingleton<ITokenService, TokenService>();
            container.RegisterSingleton<ISettingsService, SettingsService>();
            container.RegisterSingleton<IAppSettingsService, AppSettingsService>();

            // Repositories
            container.RegisterSingleton<IFolderRepository, FolderRepository>();
            container.RegisterSingleton<IFolderApiRepository, FolderApiRepository>();
            container.RegisterSingleton<ICipherRepository, CipherRepository>();
            container.RegisterSingleton<IAttachmentRepository, AttachmentRepository>();
            container.RegisterSingleton<IConnectApiRepository, ConnectApiRepository>();
            container.RegisterSingleton<IDeviceApiRepository, DeviceApiRepository>();
            container.RegisterSingleton<IAccountsApiRepository, AccountsApiRepository>();
            container.RegisterSingleton<ICipherApiRepository, CipherApiRepository>();
            container.RegisterSingleton<ISettingsRepository, SettingsRepository>();
            container.RegisterSingleton<ISettingsApiRepository, SettingsApiRepository>();
            container.RegisterSingleton<ITwoFactorApiRepository, TwoFactorApiRepository>();
            container.RegisterSingleton<ISyncApiRepository, SyncApiRepository>();
            container.RegisterSingleton<ICollectionRepository, CollectionRepository>();
            container.RegisterSingleton<ICipherCollectionRepository, CipherCollectionRepository>();

            // Other
            container.RegisterSingleton(CrossConnectivity.Current);
            container.RegisterSingleton(CrossFingerprint.Current);

            var settings = new Settings("group.com.8bit.bitwarden");
            container.RegisterSingleton<ISettings>(settings);

            Resolver.ResetResolver(new SimpleInjectorResolver(container));
        }

        private void SetCulture()
        {
            var localizeService = Resolver.Resolve<ILocalizeService>();
            var ci = localizeService.GetCurrentCultureInfo();
            AppResources.Culture = ci;
            localizeService.SetLocale(ci);
        }
    }
}