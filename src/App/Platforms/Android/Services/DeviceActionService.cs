using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.Nfc;
using Android.OS;
using Android.Provider;
using Android.Text;
using Android.Text.Method;
using Android.Views;
using Android.Views.InputMethods;
using Android.Widget;
using AndroidX.Credentials;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.App.Utilities.Prompts;
using Bit.Core.Abstractions;
using Bit.App.Droid.Utilities;
using Bit.App.Models;
using Bit.Droid.Autofill;
using Microsoft.Maui.Controls.Compatibility.Platform.Android;
using Resource = Bit.Core.Resource;
using Application = Android.App.Application;
using Bit.Core.Services;
using Bit.Core.Utilities.Fido2;

namespace Bit.Droid.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;
        private AlertDialog _progressDialog;
        object _progressDialogLock = new object();

        private Toast _toast;
        private string _userAgent;

        public DeviceActionService(
            IStateService stateService,
            IMessagingService messagingService)
        {
            _stateService = stateService;
            _messagingService = messagingService;
        }

        public string DeviceUserAgent
        {
            get
            {
                if (string.IsNullOrWhiteSpace(_userAgent))
                {
                    _userAgent = $"Bitwarden_Mobile/{AppInfo.VersionString} " +
                        $"(Android {Build.VERSION.Release}; SDK {Build.VERSION.Sdk}; Model {Build.Model})";
                }
                return _userAgent;
            }
        }

        public Bit.Core.Enums.DeviceType DeviceType => Bit.Core.Enums.DeviceType.Android;

        public void Toast(string text, bool longDuration = false)
        {
            if (_toast != null)
            {
                _toast.Cancel();
                _toast.Dispose();
                _toast = null;
            }
            _toast = Android.Widget.Toast.MakeText(Microsoft.Maui.ApplicationModel.Platform.CurrentActivity, text,
                longDuration ? ToastLength.Long : ToastLength.Short);
            _toast.Show();
        }

        public bool LaunchApp(string appName)
        {
            try
            {
                if ((int)Build.VERSION.SdkInt < 33)
                {
                    // API 33 required to avoid using wildcard app visibility or dangerous permissions
                    // https://developer.android.com/reference/android/content/pm/PackageManager#getLaunchIntentSenderForPackage(java.lang.String)
                    return false;
                }
                var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
                appName = appName.Replace("androidapp://", string.Empty);
                var launchIntentSender = activity?.PackageManager?.GetLaunchIntentSenderForPackage(appName);
                launchIntentSender?.SendIntent(activity, Result.Ok, null, null, null);
                return launchIntentSender != null;
            }
            catch (IntentSender.SendIntentException)
            {
                return false;
            }
            catch (Android.Util.AndroidException)
            {
                return false;
            }
        }

        public async Task ShowLoadingAsync(string text)
        {
            if (_progressDialog != null)
            {
                await HideLoadingAsync();
            }

            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            var inflater = (LayoutInflater)activity.GetSystemService(Context.LayoutInflaterService);
            var dialogView = inflater.Inflate(Resource.Layout.progress_dialog_layout, null);
            
            var txtLoading = dialogView.FindViewById<TextView>(Resource.Id.txtLoading);
            txtLoading.Text = text;
            txtLoading.SetTextColor(ThemeHelpers.TextColor);

            _progressDialog = new AlertDialog.Builder(activity)
                .SetView(dialogView)
                .SetCancelable(false)
                .Create();
            _progressDialog.Show();
        }

        public Task HideLoadingAsync()
        {
            // Based on https://github.com/redth-org/AndHUD/blob/master/AndHUD/AndHUD.cs
            lock (_progressDialogLock)
            {
                if (_progressDialog is null)
                {
                    return Task.CompletedTask;
                }

                void actionDismiss()
                {
                    try
                    {
                        if (IsAlive(_progressDialog) && IsAlive(_progressDialog.Window))
                        {
                            _progressDialog.Hide();
                            _progressDialog.Dismiss();
                        }
                    }
                    catch
                    {
                        // ignore
                    }

                    _progressDialog = null;
                }

                // First try the SynchronizationContext
                if (Application.SynchronizationContext != null)
                {
                    Application.SynchronizationContext.Send(state => actionDismiss(), null);
                    return Task.CompletedTask;
                }

                // Otherwise try OwnerActivity on dialog
                var ownerActivity = _progressDialog?.OwnerActivity;
                if (IsAlive(ownerActivity))
                {
                    ownerActivity.RunOnUiThread(actionDismiss);
                    return Task.CompletedTask;
                }

                // Otherwise try get it from the Window Context
                if (_progressDialog?.Window?.Context is Activity windowActivity && IsAlive(windowActivity))
                {
                    windowActivity.RunOnUiThread(actionDismiss);
                    return Task.CompletedTask;
                }

                // Finally if all else fails, let's see if current activity is MainActivity
                if (Microsoft.Maui.ApplicationModel.Platform.CurrentActivity is MainActivity activity && IsAlive(activity))
                {
                    activity.RunOnUiThread(actionDismiss);
                    return Task.CompletedTask;
                }

                return Task.CompletedTask;
            }
        }

        bool IsAlive(Java.Lang.Object @object)
        {
            if (@object == null)
                return false;

            if (@object.Handle == IntPtr.Zero)
                return false;

            if (@object is Activity activity)
            {
                if (activity.IsFinishing)
                    return false;

                if (activity.IsDestroyed)
                    return false;
            }

            return true;
        }

        public Task<string> DisplayPromptAync(string title = null, string description = null,
            string text = null, string okButtonText = null, string cancelButtonText = null,
            bool numericKeyboard = false, bool autofocus = true, bool password = false)
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);
            alertBuilder.SetMessage(description);
            var input = new EditText(activity)
            {
                InputType = InputTypes.ClassText
            };
            if (text == null)
            {
                text = string.Empty;
            }
            if (numericKeyboard)
            {
                SetNumericKeyboardTo(input);
            }
            if (password)
            {
                input.InputType = InputTypes.TextVariationPassword | InputTypes.ClassText;
            }

            input.ImeOptions = input.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning |
                (ImeAction)ImeFlags.NoExtractUi;
            input.Text = text;
            input.SetSelection(text.Length);
            var container = new FrameLayout(activity);
            var lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MatchParent,
                LinearLayout.LayoutParams.MatchParent);
            lp.SetMargins(25, 0, 25, 0);
            input.LayoutParameters = lp;
            container.AddView(input);
            alertBuilder.SetView(container);

            okButtonText = okButtonText ?? AppResources.Ok;
            cancelButtonText = cancelButtonText ?? AppResources.Cancel;
            var result = new TaskCompletionSource<string>();
            alertBuilder.SetPositiveButton(okButtonText,
                (sender, args) => result.TrySetResult(input.Text ?? string.Empty));
            alertBuilder.SetNegativeButton(cancelButtonText, (sender, args) => result.TrySetResult(null));

            var alert = alertBuilder.Create();
            alert.Window.SetSoftInputMode(Android.Views.SoftInput.StateVisible);
            alert.Show();
            if (autofocus)
            {
                input.RequestFocus();
            }
            return result.Task;
        }

        public Task<ValidatablePromptResponse?> DisplayValidatablePromptAsync(ValidatablePromptConfig config)
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null)
            {
                return Task.FromResult<ValidatablePromptResponse?>(null);
            }

            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(config.Title);
            var view = activity.LayoutInflater.Inflate(Resource.Layout.validatable_input_dialog_layout, null);
            alertBuilder.SetView(view);
            
            var result = new TaskCompletionSource<ValidatablePromptResponse?>();

            alertBuilder.SetOnCancelListener(new BasicDialogWithResultCancelListener(result));
            alertBuilder.SetPositiveButton(config.OkButtonText ?? AppResources.Ok, listener: null);
            alertBuilder.SetNegativeButton(config.CancelButtonText ?? AppResources.Cancel, (sender, args) => result.TrySetResult(null));
            if (!string.IsNullOrEmpty(config.ThirdButtonText))
            {
                alertBuilder.SetNeutralButton(config.ThirdButtonText, (sender, args) => result.TrySetResult(new ValidatablePromptResponse(null, true)));
            }

            var alert = alertBuilder.Create();

            var input = view.FindViewById<EditText>(Resource.Id.txtValue);
            var lblHeader = view.FindViewById<TextView>(Resource.Id.lblHeader);
            var lblValueSubinfo = view.FindViewById<TextView>(Resource.Id.lblValueSubinfo);

            lblHeader.Text = config.Subtitle;
            lblValueSubinfo.Text = config.ValueSubInfo;

            var defaultSubInfoColor = lblValueSubinfo.TextColors;

            input.InputType = InputTypes.ClassText;

            if (config.NumericKeyboard)
            {
                SetNumericKeyboardTo(input);
            }

            input.ImeOptions = input.ImeOptions | (ImeAction)ImeFlags.NoPersonalizedLearning | (ImeAction)ImeFlags.NoExtractUi;
            input.Text = config.Text ?? string.Empty;
            input.SetSelection(config.Text?.Length ?? 0);
            input.AfterTextChanged += (sender, args) =>
            {
                if (lblValueSubinfo.Text != config.ValueSubInfo)
                {
                    lblValueSubinfo.Text = config.ValueSubInfo;
                    lblHeader.SetTextColor(defaultSubInfoColor);
                    lblValueSubinfo.SetTextColor(defaultSubInfoColor);
                }
            };

            alert.Window.SetSoftInputMode(SoftInput.StateVisible);
            alert.Show();

            var positiveButton = alert.GetButton((int)DialogButtonType.Positive);
            positiveButton.Click += (sender, args) =>
            {
                var error = config.ValidateText(input.Text);
                if (error != null)
                {
                    lblHeader.SetTextColor(ThemeManager.GetResourceColor("DangerColor").ToAndroid());
                    lblValueSubinfo.SetTextColor(ThemeManager.GetResourceColor("DangerColor").ToAndroid());
                    lblValueSubinfo.Text = error;
                    lblValueSubinfo.SendAccessibilityEvent(Android.Views.Accessibility.EventTypes.ViewFocused);
                    return;
                }

                result.TrySetResult(new ValidatablePromptResponse(input.Text, false));
                alert.Dismiss();
            };

            return result.Task;
        }

        public void RateApp()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            try
            {
                var rateIntent = RateIntentForUrl("market://details", activity);
                activity.StartActivity(rateIntent);
            }
            catch (ActivityNotFoundException)
            {
                var rateIntent = RateIntentForUrl("https://play.google.com/store/apps/details", activity);
                activity.StartActivity(rateIntent);
            }
        }

        public string GetBuildNumber()
        {
            return Application.Context.ApplicationContext.PackageManager.GetPackageInfo(
                Application.Context.PackageName, 0).VersionCode.ToString();
        }

        public bool SupportsFaceBiometric()
        {
            // only used by iOS
            return false;
        }

        public Task<bool> SupportsFaceBiometricAsync()
        {
            // only used by iOS
            return Task.FromResult(SupportsFaceBiometric());
        }

        public bool SupportsNfc()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            var manager = activity.GetSystemService(Context.NfcService) as NfcManager;
            return manager.DefaultAdapter?.IsEnabled ?? false;
        }

        public bool SupportsCamera()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            return activity.PackageManager.HasSystemFeature(PackageManager.FeatureCamera);
        }

        public int SystemMajorVersion()
        {
            return (int)Build.VERSION.SdkInt;
        }

        public string SystemModel()
        {
            return Build.Model;
        }

        public Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons)
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null)
            {
                return Task.FromResult<string>(null);
            }

            var result = new TaskCompletionSource<string>();
            var alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.SetTitle(title);

            if (!string.IsNullOrWhiteSpace(message))
            {
                if (buttons != null && buttons.Length > 2)
                {
                    if (!string.IsNullOrWhiteSpace(title))
                    {
                        alertBuilder.SetTitle($"{title}: {message}");
                    }
                    else
                    {
                        alertBuilder.SetTitle(message);
                    }
                }
                else
                {
                    alertBuilder.SetMessage(message);
                }
            }

            if (buttons != null)
            {
                if (buttons.Length > 2)
                {
                    alertBuilder.SetItems(buttons, (sender, args) =>
                    {
                        result.TrySetResult(buttons[args.Which]);
                    });
                }
                else
                {
                    if (buttons.Length > 0)
                    {
                        alertBuilder.SetPositiveButton(buttons[0], (sender, args) =>
                        {
                            result.TrySetResult(buttons[0]);
                        });
                    }
                    if (buttons.Length > 1)
                    {
                        alertBuilder.SetNeutralButton(buttons[1], (sender, args) =>
                        {
                            result.TrySetResult(buttons[1]);
                        });
                    }
                }
            }

            if (!string.IsNullOrWhiteSpace(cancel))
            {
                alertBuilder.SetNegativeButton(cancel, (sender, args) =>
                {
                    result.TrySetResult(cancel);
                });
            }

            var alert = alertBuilder.Create();
            alert.CancelEvent += (o, args) => { result.TrySetResult(null); };
            alert.Show();
            return result.Task;
        }

        public async Task<string> DisplayActionSheetAsync(string title, string cancel, string destruction,
            params string[] buttons)
        {
            return await Microsoft.Maui.Controls.Application.Current.MainPage.DisplayActionSheet(
                title, cancel, destruction, buttons);
        }


        public void OpenAccessibilityOverlayPermissionSettings()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            try
            {
                var intent = new Intent(Settings.ActionManageOverlayPermission);
                intent.SetData(Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch (ActivityNotFoundException)
            {
                // can't open overlay permission management, fall back to app settings
                var intent = new Intent(Settings.ActionApplicationDetailsSettings);
                intent.SetData(Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch
            {
                var alertBuilder = new AlertDialog.Builder(activity);
                alertBuilder.SetMessage(AppResources.BitwardenAutofillGoToSettings);
                alertBuilder.SetCancelable(true);
                alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
                {
                    (sender as AlertDialog)?.Cancel();
                });
                alertBuilder.Create().Show();
            }
        }

        public void OpenCredentialProviderSettings()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            try
            {
                var pendingIntent = ICredentialManager.Create(activity).CreateSettingsPendingIntent();
                pendingIntent.Send();
            }
            catch (ActivityNotFoundException)
            {
                var alertBuilder = new AlertDialog.Builder(activity);
                alertBuilder.SetMessage(AppResources.BitwardenCredentialProviderGoToSettings);
                alertBuilder.SetCancelable(true);
                alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
                {
                    (sender as AlertDialog)?.Cancel();
                });
                alertBuilder.Create().Show();
            }
        }

        public void OpenAccessibilitySettings()
        {
            try
            {
                var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
                var intent = new Intent(Settings.ActionAccessibilitySettings);
                activity.StartActivity(intent);
            }
            catch { }
        }

        public void OpenAutofillSettings()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            try
            {
                var intent = new Intent(Settings.ActionRequestSetAutofillService);
                intent.SetData(Android.Net.Uri.Parse("package:com.x8bit.bitwarden"));
                activity.StartActivity(intent);
            }
            catch (ActivityNotFoundException)
            {
                var alertBuilder = new AlertDialog.Builder(activity);
                alertBuilder.SetMessage(AppResources.BitwardenAutofillGoToSettings);
                alertBuilder.SetCancelable(true);
                alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
                {
                    (sender as AlertDialog)?.Cancel();
                });
                alertBuilder.Create().Show();
            }
        }

        public long GetActiveTime()
        {
            // Returns milliseconds since the system was booted, and includes deep sleep. This clock is guaranteed to
            // be monotonic, and continues to tick even when the CPU is in power saving modes, so is the recommend
            // basis for general purpose interval timing.
            // ref: https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime()
            return SystemClock.ElapsedRealtime();
        }

        public async Task ExecuteFido2CredentialActionAsync(AppOptions appOptions)
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null || string.IsNullOrWhiteSpace(appOptions.Fido2CredentialAction))
            {
                return;
            }

            if (appOptions.Fido2CredentialAction == CredentialProviderConstants.Fido2CredentialGet)
            {
                await ExecuteFido2GetCredentialAsync(appOptions);
            }
            else if (appOptions.Fido2CredentialAction == CredentialProviderConstants.Fido2CredentialCreate)
            {
                await ExecuteFido2CreateCredentialAsync();
            }

            // Clear CredentialAction and FromFido2Framework values to avoid erratic behaviors in subsequent navigation/flows
            // For Fido2CredentialGet these are no longer needed as a new Activity will be initiated.
            // For Fido2CredentialCreate the app will rely on IFido2MakeCredentialConfirmationUserInterface.IsConfirmingNewCredential
            appOptions.Fido2CredentialAction = null;
            appOptions.FromFido2Framework = false;
        }

        private async Task ExecuteFido2GetCredentialAsync(AppOptions appOptions)
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null) 
            {
                return; 
            }

            try
            {
                var request = AndroidX.Credentials.Provider.PendingIntentHandler.RetrieveBeginGetCredentialRequest(activity.Intent);
                var response = new AndroidX.Credentials.Provider.BeginGetCredentialResponse();;
                var credentialEntries = new List<AndroidX.Credentials.Provider.CredentialEntry>();
                foreach (var option in request.BeginGetCredentialOptions.OfType<AndroidX.Credentials.Provider.BeginGetPublicKeyCredentialOption>())
                {
                    credentialEntries.AddRange(await Bit.App.Platforms.Android.Autofill.CredentialHelpers.PopulatePasskeyDataAsync(request.CallingAppInfo, option, activity, appOptions.HasUnlockedInThisTransaction));
                }

                if (credentialEntries.Any())
                {
                    response = new AndroidX.Credentials.Provider.BeginGetCredentialResponse.Builder()
                        .SetCredentialEntries(credentialEntries)
                        .Build();
                }

                var result = new Android.Content.Intent();
                AndroidX.Credentials.Provider.PendingIntentHandler.SetBeginGetCredentialResponse(result, response);
                activity.SetResult(Result.Ok, result);
                activity.Finish();
            }
            catch (Exception ex)
            {
                Bit.Core.Services.LoggerHelper.LogEvenIfCantBeResolved(ex);

                activity.SetResult(Result.Canceled);
                activity.Finish();
            }
        }

        private async Task ExecuteFido2CreateCredentialAsync()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null) { return; }

            try
            {
                var getRequest = AndroidX.Credentials.Provider.PendingIntentHandler.RetrieveProviderCreateCredentialRequest(activity.Intent);
                await Bit.App.Platforms.Android.Autofill.CredentialHelpers.CreateCipherPasskeyAsync(getRequest, activity);
            }
            catch (Exception ex)
            {
                Bit.Core.Services.LoggerHelper.LogEvenIfCantBeResolved(ex);

                activity.SetResult(Result.Canceled);
                activity.Finish();
            }
        }
        
        public void CloseMainApp()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (activity == null)
            {
                return;
            }
            activity.Finish();
            _messagingService.Send("finishMainActivity");
        }

        public bool SupportsFido2()
        {
            return true;
        }

        public bool SupportsCredentialProviderService() => Build.VERSION.SdkInt >= BuildVersionCodes.UpsideDownCake;

        public bool SupportsAutofillServices() => Build.VERSION.SdkInt >= BuildVersionCodes.O;

        public bool SupportsInlineAutofill() => Build.VERSION.SdkInt >= BuildVersionCodes.R;

        public bool SupportsDrawOver() => Build.VERSION.SdkInt >= BuildVersionCodes.M;

        private Intent RateIntentForUrl(string url, Activity activity)
        {
            var intent = new Intent(Intent.ActionView, Android.Net.Uri.Parse($"{url}?id={activity.PackageName}"));
            var flags = ActivityFlags.NoHistory | ActivityFlags.MultipleTask;
            if ((int)Build.VERSION.SdkInt >= 21)
            {
                flags |= ActivityFlags.NewDocument;
            }
            else
            {
                // noinspection deprecation
                flags |= ActivityFlags.ClearWhenTaskReset;
            }
            intent.AddFlags(flags);
            return intent;
        }

        public float GetSystemFontSizeScale()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            return activity?.Resources?.Configuration?.FontScale ?? 1;
        }
        
        public async Task OnAccountSwitchCompleteAsync()
        {
            // for any Android-specific cleanup required after switching accounts
        }

        public async Task SetScreenCaptureAllowedAsync()
        {
            var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
            if (await _stateService.GetScreenCaptureAllowedAsync())
            {
                activity.RunOnUiThread(() => activity.Window.ClearFlags(WindowManagerFlags.Secure));
                return;
            }
            activity.RunOnUiThread(() => activity.Window.AddFlags(WindowManagerFlags.Secure));
        }

        public void OpenAppSettings()
        {
            var intent = new Intent(Android.Provider.Settings.ActionApplicationDetailsSettings);
            intent.AddFlags(ActivityFlags.NewTask);
            var uri = Android.Net.Uri.FromParts("package", Application.Context.PackageName, null);
            intent.SetData(uri);
            Application.Context.StartActivity(intent);
        }

        public void CloseExtensionPopUp()
        {
            // only used by iOS
            throw new NotImplementedException();
        }

        public string GetAutofillAccessibilityDescription()
        {
            if (Build.VERSION.SdkInt <= BuildVersionCodes.LollipopMr1)
            {
                return AppResources.AccessibilityDescription;
            }
            if (Build.VERSION.SdkInt <= BuildVersionCodes.M)
            {
                return AppResources.AccessibilityDescription2;
            }
            if (Build.VERSION.SdkInt <= BuildVersionCodes.NMr1)
            {
                return AppResources.AccessibilityDescription3;
            }

            return AppResources.AccessibilityDescription4;
        }

        public string GetAutofillDrawOverDescription()
        {
            if (Build.VERSION.SdkInt <= BuildVersionCodes.M)
            {
                return AppResources.DrawOverDescription;
            }
            if (Build.VERSION.SdkInt <= BuildVersionCodes.NMr1)
            {
                return AppResources.DrawOverDescription2;
            }

            return AppResources.DrawOverDescription3;
        }

        private void SetNumericKeyboardTo(EditText editText)
        {
            editText.InputType = InputTypes.ClassNumber | InputTypes.NumberFlagDecimal | InputTypes.NumberFlagSigned;
#pragma warning disable CS0618 // Type or member is obsolete
            editText.KeyListener = DigitsKeyListener.GetInstance(false, false);
#pragma warning restore CS0618 // Type or member is obsolete
        }
    }

    class BasicDialogWithResultCancelListener : Java.Lang.Object, IDialogInterfaceOnCancelListener
    {
        private readonly TaskCompletionSource<ValidatablePromptResponse?> _taskCompletionSource;

        public BasicDialogWithResultCancelListener(TaskCompletionSource<ValidatablePromptResponse?> taskCompletionSource)
        {
            _taskCompletionSource = taskCompletionSource;
        }

        public void OnCancel(IDialogInterface dialog)
        {
            _taskCompletionSource?.TrySetResult(null);
            dialog?.Dismiss();
        }
    }
}
