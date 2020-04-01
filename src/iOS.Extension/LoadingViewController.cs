using System;
using System.Diagnostics;
using Foundation;
using UIKit;
using Bit.iOS.Core;
using Bit.iOS.Extension.Models;
using MobileCoreServices;
using Bit.iOS.Core.Utilities;
using Bit.App.Resources;
using Bit.iOS.Core.Controllers;
using System.Collections.Generic;
using Bit.iOS.Core.Models;
using Bit.Core.Utilities;
using Bit.Core.Abstractions;
using Bit.App.Abstractions;

namespace Bit.iOS.Extension
{
    public partial class LoadingViewController : ExtendedUIViewController
    {
        private Context _context = new Context();
        private bool _initedAppCenter;

        public LoadingViewController(IntPtr handle)
            : base(handle)
        { }

        public override void ViewDidLoad()
        {
            InitApp();
            base.ViewDidLoad();
            Logo.Image = new UIImage(ThemeHelpers.LightTheme ? "logo.png" : "logo_white.png");
            View.BackgroundColor = ThemeHelpers.SplashBackgroundColor;
            _context.ExtContext = ExtensionContext;
            foreach (var item in ExtensionContext.InputItems)
            {
                var processed = false;
                foreach (var itemProvider in item.Attachments)
                {
                    if (ProcessWebUrlProvider(itemProvider)
                        || ProcessFindLoginProvider(itemProvider)
                        || ProcessFindLoginBrowserProvider(itemProvider, Constants.UTTypeAppExtensionFillBrowserAction)
                        || ProcessFindLoginBrowserProvider(itemProvider, Constants.UTTypeAppExtensionFillWebViewAction)
                        || ProcessSaveLoginProvider(itemProvider)
                        || ProcessChangePasswordProvider(itemProvider)
                        || ProcessExtensionSetupProvider(itemProvider))
                    {
                        processed = true;
                        break;
                    }
                }
                if (processed)
                {
                    break;
                }
            }
        }

        public override void ViewDidAppear(bool animated)
        {
            base.ViewDidAppear(animated);
            if (!IsAuthed())
            {
                var alert = Dialogs.CreateAlert(null, AppResources.MustLogInMainApp, AppResources.Ok, (a) =>
                {
                    CompleteRequest(null, null);
                });
                PresentViewController(alert, true, null);
                return;
            }
            if (_context.ProviderType == Constants.UTTypeAppExtensionSetup)
            {
                PerformSegue("setupSegue", this);
                return;
            }
            if (IsLocked())
            {
                PerformSegue("lockPasswordSegue", this);
            }
            else
            {
                ContinueOn();
            }
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is LoginListViewController listLoginController)
                {
                    listLoginController.Context = _context;
                    listLoginController.LoadingController = this;
                }
                else if (navController.TopViewController is LoginAddViewController addLoginController)
                {
                    addLoginController.Context = _context;
                    addLoginController.LoadingController = this;
                }
                else if (navController.TopViewController is LockPasswordViewController passwordViewController)
                {
                    passwordViewController.LoadingController = this;
                }
                else if (navController.TopViewController is SetupViewController setupViewController)
                {
                    setupViewController.Context = _context;
                    setupViewController.LoadingController = this;
                }
            }
        }

        public void DismissLockAndContinue()
        {
            Debug.WriteLine("BW Log, Dismissing lock controller.");
            DismissViewController(false, () => ContinueOn());
        }

        private void ContinueOn()
        {
            Debug.WriteLine("BW Log, Segue to setup, login add or list.");
            if (_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
            {
                PerformSegue("newLoginSegue", this);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionSetup)
            {
                PerformSegue("setupSegue", this);
            }
            else
            {
                PerformSegue("loginListSegue", this);
            }
        }

        public void CompleteUsernamePasswordRequest(string id, string username, string password,
            List<Tuple<string, string>> fields, string totp)
        {
            NSDictionary itemData = null;
            if (_context.ProviderType == UTType.PropertyList)
            {
                var fillScript = new FillScript(_context.Details, username, password, fields);
                var scriptJson = CoreHelpers.SerializeJson(fillScript, true);
                var scriptDict = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
                itemData = new NSDictionary(NSJavaScriptExtension.FinalizeArgumentKey, scriptDict);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionFindLoginAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionUsernameKey, username,
                    Constants.AppExtensionPasswordKey, password);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionFillBrowserAction
                || _context.ProviderType == Constants.UTTypeAppExtensionFillWebViewAction)
            {
                var fillScript = new FillScript(_context.Details, username, password, fields);
                var scriptJson = CoreHelpers.SerializeJson(fillScript, true);
                itemData = new NSDictionary(Constants.AppExtensionWebViewPageFillScript, scriptJson);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionSaveLoginAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionUsernameKey, username,
                    Constants.AppExtensionPasswordKey, password);
            }
            else if (_context.ProviderType == Constants.UTTypeAppExtensionChangePasswordAction)
            {
                itemData = new NSDictionary(
                    Constants.AppExtensionPasswordKey, string.Empty,
                    Constants.AppExtensionOldPasswordKey, password);
            }

            if (!string.IsNullOrWhiteSpace(totp))
            {
                UIPasteboard.General.String = totp;
            }
            CompleteRequest(id, itemData);
        }

        public void CompleteRequest(string id, NSDictionary itemData)
        {
            Debug.WriteLine("BW LOG, itemData: " + itemData);
            var resultsProvider = new NSItemProvider(itemData, UTType.PropertyList);
            var resultsItem = new NSExtensionItem { Attachments = new NSItemProvider[] { resultsProvider } };
            var returningItems = new NSExtensionItem[] { resultsItem };
            NSRunLoop.Main.BeginInvokeOnMainThread(async () =>
            {
                if (!string.IsNullOrWhiteSpace(id) && itemData != null)
                {
                    var eventService = ServiceContainer.Resolve<IEventService>("eventService");
                    await eventService.CollectAsync(Bit.Core.Enums.EventType.Cipher_ClientAutofilled, id);
                }
                ServiceContainer.Reset();
                ExtensionContext?.CompleteRequest(returningItems, null);
            });
        }

        private bool ProcessItemProvider(NSItemProvider itemProvider, string type, Action<NSDictionary> dictAction,
            Action<NSUrl> urlAction = null)
        {
            if (!itemProvider.HasItemConformingTo(type))
            {
                return false;
            }

            itemProvider.LoadItem(type, null, (NSObject list, NSError error) =>
            {
                if (list == null)
                {
                    return;
                }

                _context.ProviderType = type;
                if (list is NSDictionary dict && dictAction != null)
                {
                    dictAction(dict);
                }
                else if (list is NSUrl && urlAction != null)
                {
                    var url = list as NSUrl;
                    urlAction(url);
                }
                else
                {
                    throw new Exception("Cannot parse list for action. List is " +
                        (list?.GetType().ToString() ?? "null"));
                }

                Debug.WriteLine("BW LOG, ProviderType: " + _context.ProviderType);
                Debug.WriteLine("BW LOG, Url: " + _context.UrlString);
                Debug.WriteLine("BW LOG, Title: " + _context.LoginTitle);
                Debug.WriteLine("BW LOG, Username: " + _context.Username);
                Debug.WriteLine("BW LOG, Password: " + _context.Password);
                Debug.WriteLine("BW LOG, Old Password: " + _context.OldPassword);
                Debug.WriteLine("BW LOG, Notes: " + _context.Notes);
                Debug.WriteLine("BW LOG, Details: " + _context.Details);

                if (_context.PasswordOptions != null)
                {
                    Debug.WriteLine("BW LOG, PasswordOptions Min Length: " + _context.PasswordOptions.MinLength);
                    Debug.WriteLine("BW LOG, PasswordOptions Max Length: " + _context.PasswordOptions.MaxLength);
                    Debug.WriteLine("BW LOG, PasswordOptions Require Digits: " + _context.PasswordOptions.RequireDigits);
                    Debug.WriteLine("BW LOG, PasswordOptions Require Symbols: " + _context.PasswordOptions.RequireSymbols);
                    Debug.WriteLine("BW LOG, PasswordOptions Forbidden Chars: " + _context.PasswordOptions.ForbiddenCharacters);
                }
            });

            return true;
        }

        private bool ProcessWebUrlProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, UTType.PropertyList, dict =>
            {
                var result = dict[NSJavaScriptExtension.PreprocessingResultsKey];
                if (result == null)
                {
                    return;
                }
                _context.UrlString = result.ValueForKey(new NSString(Constants.AppExtensionUrlStringKey)) as NSString;
                var jsonStr = result.ValueForKey(new NSString(Constants.AppExtensionWebViewPageDetails)) as NSString;
                _context.Details = DeserializeString<PageDetails>(jsonStr);
            });
        }

        private bool ProcessFindLoginProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, Constants.UTTypeAppExtensionFindLoginAction, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                if (url != null)
                {
                    _context.UrlString = url;
                }
            });
        }

        private bool ProcessFindLoginBrowserProvider(NSItemProvider itemProvider, string action)
        {
            return ProcessItemProvider(itemProvider, action, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                if (url != null)
                {
                    _context.UrlString = url;
                }
                _context.Details = DeserializeDictionary<PageDetails>(dict[Constants.AppExtensionWebViewPageDetails] as NSDictionary);
            }, url =>
            {
                if (url != null)
                {
                    _context.UrlString = url.AbsoluteString;
                }
            });
        }

        private bool ProcessSaveLoginProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, Constants.UTTypeAppExtensionSaveLoginAction, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                var title = dict[Constants.AppExtensionTitleKey] as NSString;
                var sectionTitle = dict[Constants.AppExtensionSectionTitleKey] as NSString;
                var username = dict[Constants.AppExtensionUsernameKey] as NSString;
                var password = dict[Constants.AppExtensionPasswordKey] as NSString;
                var notes = dict[Constants.AppExtensionNotesKey] as NSString;
                var fields = dict[Constants.AppExtensionFieldsKey] as NSDictionary;
                if (url != null)
                {
                    _context.UrlString = url;
                }
                _context.LoginTitle = title;
                _context.Username = username;
                _context.Password = password;
                _context.Notes = notes;
                _context.PasswordOptions = DeserializeDictionary<PasswordGenerationOptions>(dict[Constants.AppExtensionPasswordGeneratorOptionsKey] as NSDictionary);
            });
        }

        private bool ProcessChangePasswordProvider(NSItemProvider itemProvider)
        {
            return ProcessItemProvider(itemProvider, Constants.UTTypeAppExtensionChangePasswordAction, dict =>
            {
                var version = dict[Constants.AppExtensionVersionNumberKey] as NSNumber;
                var url = dict[Constants.AppExtensionUrlStringKey] as NSString;
                var title = dict[Constants.AppExtensionTitleKey] as NSString;
                var sectionTitle = dict[Constants.AppExtensionSectionTitleKey] as NSString;
                var username = dict[Constants.AppExtensionUsernameKey] as NSString;
                var password = dict[Constants.AppExtensionPasswordKey] as NSString;
                var oldPassword = dict[Constants.AppExtensionOldPasswordKey] as NSString;
                var notes = dict[Constants.AppExtensionNotesKey] as NSString;
                var fields = dict[Constants.AppExtensionFieldsKey] as NSDictionary;
                if (url != null)
                {
                    _context.UrlString = url;
                }
                _context.LoginTitle = title;
                _context.Username = username;
                _context.Password = password;
                _context.OldPassword = oldPassword;
                _context.Notes = notes;
                _context.PasswordOptions = DeserializeDictionary<PasswordGenerationOptions>(dict[Constants.AppExtensionPasswordGeneratorOptionsKey] as NSDictionary);
            });
        }

        private bool ProcessExtensionSetupProvider(NSItemProvider itemProvider)
        {
            if (itemProvider.HasItemConformingTo(Constants.UTTypeAppExtensionSetup))
            {
                _context.ProviderType = Constants.UTTypeAppExtensionSetup;
                return true;
            }
            return false;
        }

        private T DeserializeDictionary<T>(NSDictionary dict)
        {
            if (dict != null)
            {
                var jsonData = NSJsonSerialization.Serialize(
                    dict, NSJsonWritingOptions.PrettyPrinted, out NSError jsonError);
                if (jsonData != null)
                {
                    var jsonString = new NSString(jsonData, NSStringEncoding.UTF8);
                    return DeserializeString<T>(jsonString);
                }
            }
            return default(T);
        }

        private T DeserializeString<T>(NSString jsonString)
        {
            if (jsonString != null)
            {
                var convertedObject = CoreHelpers.DeserializeJson<T>(jsonString.ToString());
                return convertedObject;
            }
            return default(T);
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
    }
}
