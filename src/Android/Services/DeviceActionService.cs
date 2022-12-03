using System;
using System.Threading.Tasks;
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
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Bit.Droid.Utilities;
using Plugin.CurrentActivity;

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
                    _userAgent = $"Bitwarden_Mobile/{Xamarin.Essentials.AppInfo.VersionString} " +
                        $"(Android {Build.VERSION.Release}; SDK {Build.VERSION.Sdk}; Model {Build.Model})";
                }
                return _userAgent;
            }
        }

        public DeviceType DeviceType => DeviceType.Android;

        public void Toast(string text, bool longDuration = false)
        {
            if (_toast != null)
            {
                _toast.Cancel();
                _toast.Dispose();
                _toast = null;
            }
            _toast = Android.Widget.Toast.MakeText(CrossCurrentActivity.Current.Activity, text,
                longDuration ? ToastLength.Long : ToastLength.Short);
            _toast.Show();
        }

        public bool LaunchApp(string appName)
        {
            if ((int)Build.VERSION.SdkInt < 33)
            {
                // API 33 required to avoid using wildcard app visibility or dangerous permissions
                // https://developer.android.com/reference/android/content/pm/PackageManager#getLaunchIntentSenderForPackage(java.lang.String)
                return false;
            }
            var activity = CrossCurrentActivity.Current.Activity;
            appName = appName.Replace("androidapp://", string.Empty);
            var launchIntentSender = activity?.PackageManager?.GetLaunchIntentSenderForPackage(appName);
            launchIntentSender?.SendIntent(activity, Result.Ok, null, null, null);
            return launchIntentSender != null;
        }

        public async Task ShowLoadingAsync(string text)
        {
            if (_progressDialog != null)
            {
                await HideLoadingAsync();
            }

            var activity = CrossCurrentActivity.Current.Activity;
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
                if (CrossCurrentActivity.Current.Activity is MainActivity activity && IsAlive(activity))
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
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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
                input.InputType = InputTypes.ClassNumber | InputTypes.NumberFlagDecimal | InputTypes.NumberFlagSigned;
#pragma warning disable CS0618 // Type or member is obsolete
                input.KeyListener = DigitsKeyListener.GetInstance(false, false);
#pragma warning restore CS0618 // Type or member is obsolete
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

        public void RateApp()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
            var manager = activity.GetSystemService(Context.NfcService) as NfcManager;
            return manager.DefaultAdapter?.IsEnabled ?? false;
        }

        public bool SupportsCamera()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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
            return await Xamarin.Forms.Application.Current.MainPage.DisplayActionSheet(
                title, cancel, destruction, buttons);
        }


        public void OpenAccessibilityOverlayPermissionSettings()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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

        public void OpenAccessibilitySettings()
        {
            try
            {
                var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
                var intent = new Intent(Settings.ActionAccessibilitySettings);
                activity.StartActivity(intent);
            }
            catch { }
        }

        public void OpenAutofillSettings()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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
        
        public void CloseMainApp()
        {
            var activity = (MainActivity)CrossCurrentActivity.Current.Activity;
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
            var activity = CrossCurrentActivity.Current?.Activity as MainActivity;
            return activity?.Resources?.Configuration?.FontScale ?? 1;
        }
        
        public async Task OnAccountSwitchCompleteAsync()
        {
            // for any Android-specific cleanup required after switching accounts
        }

        public async Task SetScreenCaptureAllowedAsync()
        {
            if (CoreHelpers.ForceScreenCaptureEnabled())
            {
                return;
            }

            var activity = CrossCurrentActivity.Current?.Activity;
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
    }
}
