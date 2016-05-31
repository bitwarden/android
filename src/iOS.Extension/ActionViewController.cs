using System;
using System.Diagnostics;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Repositories;
using Bit.App.Services;
using Bit.iOS.Core.Services;
using CoreGraphics;
using Foundation;
using Microsoft.Practices.Unity;
using MobileCoreServices;
using UIKit;
using XLabs.Ioc;
using XLabs.Ioc.Unity;

namespace Bit.iOS.Extension
{
    public partial class ActionViewController : UIViewController
    {
        private const string AppExtensionVersionNumberKey = "version_number";

        private const string AppExtensionUrlStringKey = "url_string";
        private const string AppExtensionUsernameKey = "username";
        private const string AppExtensionPasswordKey = "password";
        private const string AppExtensionTotpKey = "totp";
        private const string AppExtensionTitleKey = "login_title";
        private const string AppExtensionNotesKey = "notes";
        private const string AppExtensionSectionTitleKey = "section_title";
        private const string AppExtensionFieldsKey = "fields";
        private const string AppExtensionReturnedFieldsKey = "returned_fields";
        private const string AppExtensionOldPasswordKey = "old_password";
        private const string AppExtensionPasswordGeneratorOptionsKey = "password_generator_options";

        private const string AppExtensionGeneratedPasswordMinLengthKey = "password_min_length";
        private const string AppExtensionGeneratedPasswordMaxLengthKey = "password_max_length";
        private const string AppExtensionGeneratedPasswordRequireDigitsKey = "password_require_digits";
        private const string AppExtensionGeneratedPasswordRequireSymbolsKey = "password_require_symbols";
        private const string AppExtensionGeneratedPasswordForbiddenCharactersKey = "password_forbidden_characters";

        private const string UTTypeAppExtensionFindLoginAction = "org.appextension.find-login-action";
        private const string UTTypeAppExtensionSaveLoginAction = "org.appextension.save-login-action";
        private const string UTTypeAppExtensionChangePasswordAction = "org.appextension.change-password-action";
        private const string UTTypeAppExtensionFillWebViewAction = "org.appextension.fill-webview-action";
        private const string UTTypeAppExtensionFillBrowserAction = "org.appextension.fill-browser-action";

        public ActionViewController() : base("ActionViewController", null)
        {
            if(!Resolver.IsSet)
            {
                SetIoc();
            }
        }

        public string ProviderType { get; set; }
        public Uri Url { get; set; }
        public string SiteTitle { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string OldPassword { get; set; }
        public string Notes { get; set; }
        public PasswordGenerationOptions PasswordOptions { get; set; }

        private void SetIoc()
        {
            var container = new UnityContainer();

            container
                // Services
                .RegisterType<IDatabaseService, DatabaseService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISqlService, SqlService>(new ContainerControlledLifetimeManager())
                //.RegisterType<ISecureStorageService, KeyChainStorageService>(new ContainerControlledLifetimeManager())
                .RegisterType<ICryptoService, CryptoService>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthService, AuthService>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderService, FolderService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteService, SiteService>(new ContainerControlledLifetimeManager())
                .RegisterType<ISyncService, SyncService>(new ContainerControlledLifetimeManager())
                //.RegisterType<IClipboardService, ClipboardService>(new ContainerControlledLifetimeManager())
                // Repositories
                .RegisterType<IFolderRepository, FolderRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IFolderApiRepository, FolderApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteRepository, SiteRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<ISiteApiRepository, SiteApiRepository>(new ContainerControlledLifetimeManager())
                .RegisterType<IAuthApiRepository, AuthApiRepository>(new ContainerControlledLifetimeManager());
            // Other
            //.RegisterInstance(CrossSettings.Current, new ContainerControlledLifetimeManager())
            //.RegisterInstance(CrossConnectivity.Current, new ContainerControlledLifetimeManager())
            //.RegisterInstance(UserDialogs.Instance, new ContainerControlledLifetimeManager())
            //.RegisterInstance(CrossFingerprint.Current, new ContainerControlledLifetimeManager());

            Resolver.SetResolver(new UnityResolver(container));
        }

        public override void DidReceiveMemoryWarning()
        {
            base.DidReceiveMemoryWarning();
        }

        public override void LoadView()
        {
            foreach(var item in ExtensionContext.InputItems)
            {
                foreach(var itemProvider in item.Attachments)
                {
                    if(ProcessWebUrlProvider(itemProvider))
                    {
                        break;
                    }
                    else if(ProcessFindLoginProvider(itemProvider))
                    {
                        break;
                    }
                    else if(ProcessSaveLoginProvider(itemProvider))
                    {
                        break;
                    }
                    else if(ProcessChangePasswordProvider(itemProvider))
                    {
                        break;
                    }
                }
            }

            View = new UIView(new CGRect(x: 0.0, y: 0, width: 320.0, height: 200.0));
            var button = new UIButton(new CGRect(x: 10.0, y: 50.0, width: 200.0, height: 30.0));
            button.SetTitle("Done", UIControlState.Normal);
            button.TouchUpInside += Button_TouchUpInside;
            View.AddSubview(button);
        }

        private void Button_TouchUpInside(object sender, EventArgs e)
        {
            NSDictionary itemData = null;
            if(ProviderType == UTType.PropertyList)
            {
                itemData = new NSDictionary(
                    "username", "me@example.com",
                    "password", "mypassword",
                    "autoSubmit", true);
            }
            else if(ProviderType == UTTypeAppExtensionFindLoginAction)
            {
                itemData = new NSDictionary(
                    AppExtensionUsernameKey, "me@example.com",
                    AppExtensionPasswordKey, "mypassword");
            }
            else if(ProviderType == UTTypeAppExtensionSaveLoginAction)
            {
                itemData = new NSDictionary(
                    AppExtensionUsernameKey, "me@example.com",
                    AppExtensionPasswordKey, "mypassword");
            }
            else if(ProviderType == UTTypeAppExtensionChangePasswordAction)
            {
                itemData = new NSDictionary(
                    AppExtensionPasswordKey, "mynewpassword",
                    AppExtensionOldPasswordKey, "myoldpassword");
            }
            else
            {
                return;
            }

            var resultsProvider = new NSItemProvider(itemData, UTType.PropertyList);
            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };

            ExtensionContext.CompleteRequest(returningItems, null);
        }

        public override void ViewDidLoad()
        {
            base.ViewDidLoad();
        }

        private bool ProcessItemProvider(NSItemProvider itemProvider, string type, Action<NSDictionary> action)
        {
            if(!itemProvider.HasItemConformingTo(type))
            {
                return false;
            }

            itemProvider.LoadItem(type, null, (NSObject list, NSError error) =>
            {
                if(list == null)
                {
                    return;
                }

                ProviderType = type;
                var dict = list as NSDictionary;
                action(dict);

                Debug.WriteLine("BW LOG, ProviderType: " + ProviderType);
                Debug.WriteLine("BW LOG, Url: " + Url);
                Debug.WriteLine("BW LOG, Title: " + SiteTitle);
                Debug.WriteLine("BW LOG, Username: " + Username);
                Debug.WriteLine("BW LOG, Password: " + Password);
                Debug.WriteLine("BW LOG, Old Password: " + OldPassword);
                Debug.WriteLine("BW LOG, Notes: " + Notes);

                if(PasswordOptions != null)
                {
                    Debug.WriteLine("BW LOG, PasswordOptions Min Length: " + PasswordOptions.MinLength);
                    Debug.WriteLine("BW LOG, PasswordOptions Max Length: " + PasswordOptions.MaxLength);
                    Debug.WriteLine("BW LOG, PasswordOptions Require Digits: " + PasswordOptions.RequireDigits);
                    Debug.WriteLine("BW LOG, PasswordOptions Require Symbols: " + PasswordOptions.RequireSymbols);
                    Debug.WriteLine("BW LOG, PasswordOptions Forbidden Chars: " + PasswordOptions.ForbiddenCharacters);
                }
            });

            return true;
        }

        private bool ProcessWebUrlProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, UTType.PropertyList, (dict) =>
            {
                var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
                if(result == null)
                {
                    return;
                }

                Url = new Uri(result.ValueForKey(new NSString("url")) as NSString);
            });
        }

        private bool ProcessFindLoginProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, UTTypeAppExtensionFindLoginAction, (dict) =>
            {
                var version = dict[AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[AppExtensionUrlStringKey] as NSString;

                if(url != null)
                {
                    Url = new Uri(url);
                }
            });
        }

        private bool ProcessSaveLoginProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, UTTypeAppExtensionSaveLoginAction, (dict) =>
            {
                var version = dict[AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[AppExtensionUrlStringKey] as NSString;
                var title = dict[AppExtensionTitleKey] as NSString;
                var sectionTitle = dict[AppExtensionSectionTitleKey] as NSString;
                var username = dict[AppExtensionUsernameKey] as NSString;
                var password = dict[AppExtensionPasswordKey] as NSString;
                var notes = dict[AppExtensionNotesKey] as NSString;
                var fields = dict[AppExtensionFieldsKey] as NSDictionary;
                var passwordGenerationOptions = dict[AppExtensionPasswordGeneratorOptionsKey] as NSDictionary;

                if(url != null)
                {
                    Url = new Uri(url);
                }

                Url = new Uri(url);
                SiteTitle = title;
                Username = username;
                Password = password;
                Notes = notes;
                PasswordOptions = new PasswordGenerationOptions(passwordGenerationOptions);
            });
        }

        private bool ProcessChangePasswordProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, UTTypeAppExtensionChangePasswordAction, (dict) =>
            {
                var version = dict[AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[AppExtensionUrlStringKey] as NSString;
                var title = dict[AppExtensionTitleKey] as NSString;
                var sectionTitle = dict[AppExtensionSectionTitleKey] as NSString;
                var username = dict[AppExtensionUsernameKey] as NSString;
                var password = dict[AppExtensionPasswordKey] as NSString;
                var oldPassword = dict[AppExtensionOldPasswordKey] as NSString;
                var notes = dict[AppExtensionNotesKey] as NSString;
                var fields = dict[AppExtensionFieldsKey] as NSDictionary;
                var passwordGenerationOptions = dict[AppExtensionPasswordGeneratorOptionsKey] as NSDictionary;

                if(url != null)
                {
                    Url = new Uri(url);
                }

                SiteTitle = title;
                Username = username;
                Password = password;
                OldPassword = oldPassword;
                Notes = notes;
                PasswordOptions = new PasswordGenerationOptions(passwordGenerationOptions);
            });
        }

        public class PasswordGenerationOptions
        {
            public PasswordGenerationOptions(NSDictionary dict)
            {
                if(dict == null)
                {
                    throw new ArgumentNullException(nameof(dict));
                }

                MinLength = (dict[AppExtensionGeneratedPasswordMinLengthKey] as NSNumber)?.Int32Value ?? 0;
                MaxLength = (dict[AppExtensionGeneratedPasswordMaxLengthKey] as NSNumber)?.Int32Value ?? 0;
                RequireDigits = (dict[AppExtensionGeneratedPasswordRequireDigitsKey] as NSNumber)?.BoolValue ?? false;
                RequireSymbols = (dict[AppExtensionGeneratedPasswordRequireSymbolsKey] as NSNumber)?.BoolValue ?? false;
                ForbiddenCharacters = (dict[AppExtensionGeneratedPasswordForbiddenCharactersKey] as NSString)?.ToString();
            }

            public int MinLength { get; set; }
            public int MaxLength { get; set; }
            public bool RequireDigits { get; set; }
            public bool RequireSymbols { get; set; }
            public string ForbiddenCharacters { get; set; }
        }
    }
}