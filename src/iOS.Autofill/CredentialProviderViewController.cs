using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Resources;
using Bit.App.Services;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Services;
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
        private Context _context = new Context();

        public CredentialProviderViewController (IntPtr handle) : base (handle)
        {
        }

        public override void ViewDidLoad()
        {
            SetIoc();
            SetCulture();
            base.ViewDidLoad();
            _context.ExtContext = ExtensionContext;

            // TODO: HockeyApp
        }

        public override void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers)
        {
            System.Diagnostics.Debug.WriteLine("AUTOFILL Got identifiers " + serviceIdentifiers.Length);
            base.PrepareCredentialList(serviceIdentifiers);
        }

        public override void ProvideCredentialWithoutUserInteraction(ASPasswordCredentialIdentity credentialIdentity)
        {
            base.ProvideCredentialWithoutUserInteraction(credentialIdentity);
        }

        public override void PrepareInterfaceToProvideCredential(ASPasswordCredentialIdentity credentialIdentity)
        {
            base.PrepareInterfaceToProvideCredential(credentialIdentity);
        }

        public override void PrepareInterfaceForExtensionConfiguration()
        {
            base.PrepareInterfaceForExtensionConfiguration();
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