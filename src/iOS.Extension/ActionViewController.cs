using System;
using System.Collections.Generic;
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
using Newtonsoft.Json;
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

        private const string AppExtensionWebViewPageFillScript = "fillScript";
        private const string AppExtensionWebViewPageDetails = "pageDetails";

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
        public PageDetails Details { get; set; }

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

        public override void LoadView()
        {
            foreach(var item in ExtensionContext.InputItems)
            {
                var processed = false;
                foreach(var itemProvider in item.Attachments)
                {
                    if(ProcessWebUrlProvider(itemProvider)
                        || ProcessFindLoginProvider(itemProvider)
                        || ProcessFindLoginBrowserProvider(itemProvider, UTTypeAppExtensionFillBrowserAction)
                        || ProcessFindLoginBrowserProvider(itemProvider, UTTypeAppExtensionFillWebViewAction)
                        || ProcessSaveLoginProvider(itemProvider)
                        || ProcessChangePasswordProvider(itemProvider))
                    {
                        processed = true;
                        break;
                    }
                }

                if(processed)
                {
                    break;
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
            if(ProviderType == UTTypeAppExtensionFindLoginAction)
            {
                itemData = new NSDictionary(
                    AppExtensionUsernameKey, "me@example.com",
                    AppExtensionPasswordKey, "mypassword");
            }
            else if(ProviderType == UTType.PropertyList
                || ProviderType == UTTypeAppExtensionFillBrowserAction 
                || ProviderType == UTTypeAppExtensionFillWebViewAction)
            {
                var fillScript = new FillScript(Details);
                var scriptJson = JsonConvert.SerializeObject(fillScript);
                itemData = new NSDictionary(AppExtensionWebViewPageFillScript, scriptJson);
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
                Debug.WriteLine("BW LOG, Details: " + Details);

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

                Url = new Uri(result.ValueForKey(new NSString(AppExtensionUrlStringKey)) as NSString);
                var jsonStr = result.ValueForKey(new NSString(AppExtensionWebViewPageDetails)) as NSString;
                Details = DeserializeString<PageDetails>(jsonStr);
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

        private bool ProcessFindLoginBrowserProvider(NSItemProvider itemProvider, string action)
        {
            return ProcessItemProvider(itemProvider, action, (dict) =>
            {
                var version = dict[AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[AppExtensionUrlStringKey] as NSString;
                if(url != null)
                {
                    Url = new Uri(url);
                }

                Details = DeserializeDictionary<PageDetails>(dict[AppExtensionWebViewPageDetails] as NSDictionary);
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

                if(url != null)
                {
                    Url = new Uri(url);
                }

                Url = new Uri(url);
                SiteTitle = title;
                Username = username;
                Password = password;
                Notes = notes;
                PasswordOptions = DeserializeDictionary<PasswordGenerationOptions>(dict[AppExtensionPasswordGeneratorOptionsKey] as NSDictionary);
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

                if(url != null)
                {
                    Url = new Uri(url);
                }

                SiteTitle = title;
                Username = username;
                Password = password;
                OldPassword = oldPassword;
                Notes = notes;
                PasswordOptions = DeserializeDictionary<PasswordGenerationOptions>(dict[AppExtensionPasswordGeneratorOptionsKey] as NSDictionary);
            });
        }

        private T DeserializeDictionary<T>(NSDictionary dict)
        {
            if(dict != null)
            {
                NSError jsonError;
                var jsonData = NSJsonSerialization.Serialize(dict, NSJsonWritingOptions.PrettyPrinted, out jsonError);
                if(jsonData != null)
                {
                    var jsonString = new NSString(jsonData, NSStringEncoding.UTF8);
                    return DeserializeString<T>(jsonString);
                }
            }

            return default(T);
        }

        private T DeserializeString<T>(NSString jsonString)
        {
            if(jsonString != null)
            {
                var convertedObject = JsonConvert.DeserializeObject<T>(jsonString.ToString());
                return convertedObject;
            }

            return default(T);
        }

        public class PasswordGenerationOptions
        {
            public int MinLength { get; set; }
            public int MaxLength { get; set; }
            public bool RequireDigits { get; set; }
            public bool RequireSymbols { get; set; }
            public string ForbiddenCharacters { get; set; }
        }

        public class PageDetails
        {
            public string DocumentUUID { get; set; }
            public string Title { get; set; }
            public string Url { get; set; }
            public string DocumentUrl { get; set; }
            public string TabUrl { get; set; }
            public Dictionary<string, Form> Forms { get; set; }
            public List<Field> Fields { get; set; }
            public long CollectedTimestamp { get; set; }

            public class Form
            {
                public string OpId { get; set; }
                public string HtmlName { get; set; }
                public string HtmlId { get; set; }
                public string HtmlAction { get; set; }
                public string HtmlMethod { get; set; }
            }

            public class Field
            {
                public string OpId { get; set; }
                public int ElementNumber { get; set; }
                public bool Visible { get; set; }
                public bool Viewable { get; set; }
                public string HtmlId { get; set; }
                public string HtmlName { get; set; }
                public string HtmlClass { get; set; }
                public string LabelRight { get; set; }
                public string LabelLeft { get; set; }
                public string Type { get; set; }
                public string Value { get; set; }
                public bool Disabled { get; set; }
                public bool Readonly { get; set; }
                public string OnePasswordFieldType { get; set; }
                public string Form { get; set; }
            }
        }

        public class FillScript
        {
            public FillScript(PageDetails pageDetails)
            {
                if(pageDetails == null)
                {
                    return;
                }

                DocumentUUID = pageDetails.DocumentUUID;

                var loginForm = pageDetails.Forms.FirstOrDefault(form => pageDetails.Fields.Any(f => f.Form == form.Key && f.Type == "password")).Value;
                if(loginForm == null)
                {
                    return;
                }

                Script = new List<List<string>>();

                var password = pageDetails.Fields.FirstOrDefault(f =>
                    f.Form == loginForm.OpId
                    && f.Type == "password");

                var username = pageDetails.Fields.LastOrDefault(f =>
                    f.Form == loginForm.OpId
                    && (f.Type == "text" || f.Type == "email")
                    && f.ElementNumber < password.ElementNumber);

                if(username != null)
                {
                    Script.Add(new List<string> { "click_on_opid", username.OpId });
                    Script.Add(new List<string> { "fill_by_opid", username.OpId, "me@example.com" });
                }

                Script.Add(new List<string> { "click_on_opid", password.OpId });
                Script.Add(new List<string> { "fill_by_opid", password.OpId, "mypassword" });

                if(loginForm.HtmlAction != null)
                {
                    AutoSubmit = new Submit { FocusOpId = password.OpId };
                }
            }

            [JsonProperty(PropertyName = "script")]
            public List<List<string>> Script { get; set; }
            [JsonProperty(PropertyName = "autosubmit")]
            public Submit AutoSubmit { get; set; }
            [JsonProperty(PropertyName = "documentUUID")]
            public object DocumentUUID { get; set; }
            [JsonProperty(PropertyName = "properties")]
            public object Properties { get; set; } = new object();
            [JsonProperty(PropertyName = "options")]
            public object Options { get; set; } = new object();
            [JsonProperty(PropertyName = "metadata")]
            public object MetaData { get; set; } = new object();

            public class Submit
            {
                [JsonProperty(PropertyName = "focusOpid")]
                public string FocusOpId { get; set; }
            }
        }
    }
}