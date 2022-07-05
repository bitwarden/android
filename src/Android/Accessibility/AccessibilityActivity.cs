using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using System;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.Droid.Utilities;

namespace Bit.Droid.Accessibility
{
    [Activity(Theme = "@style/BaseTheme", WindowSoftInputMode = SoftInput.StateHidden)]
    public class AccessibilityActivity : Activity
    {
        private DateTime? _lastLaunch = null;
        private string _lastQueriedUri;

        protected override void OnCreate(Bundle bundle)
        {
            Intent?.Validate();
            base.OnCreate(bundle);
            HandleIntent(Intent, 932473);
        }

        protected override void OnNewIntent(Intent intent)
        {
            base.OnNewIntent(intent);
            HandleIntent(intent, 489729);
        }

        protected override void OnDestroy()
        {
            base.OnDestroy();
        }

        protected override void OnResume()
        {
            base.OnResume();
            if (!Intent.HasExtra("uri"))
            {
                Finish();
                return;
            }
            Intent.RemoveExtra("uri");
        }

        protected override void OnActivityResult(int requestCode, [GeneratedEnum] Result resultCode, Intent data)
        {
            base.OnActivityResult(requestCode, resultCode, data);
            if (data == null)
            {
                AccessibilityHelpers.LastCredentials = null;
            }
            else
            {
                try
                {
                    if (data.GetStringExtra("canceled") != null)
                    {
                        AccessibilityHelpers.LastCredentials = null;
                    }
                    else
                    {
                        var uri = data.GetStringExtra("uri");
                        var username = data.GetStringExtra("username");
                        var password = data.GetStringExtra("password");
                        AccessibilityHelpers.LastCredentials = new Credentials
                        {
                            Username = username,
                            Password = password,
                            Uri = uri,
                            LastUri = _lastQueriedUri
                        };
                    }
                }
                catch
                {
                    AccessibilityHelpers.LastCredentials = null;
                }
            }
            Finish();
        }

        private void HandleIntent(Intent callingIntent, int requestCode)
        {
            if (callingIntent?.GetBooleanExtra("autofillTileClicked", false) ?? false)
            {
                Intent.RemoveExtra("autofillTileClicked");
                var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
                messagingService.Send("OnAutofillTileClick");
                Finish();
            }
            else
            {
                LaunchMainActivity(callingIntent, requestCode);
            }
        }

        private void LaunchMainActivity(Intent callingIntent, int requestCode)
        {
            _lastQueriedUri = callingIntent?.GetStringExtra("uri");
            if (_lastQueriedUri == null)
            {
                Finish();
                return;
            }
            var now = DateTime.UtcNow;
            if (_lastLaunch.HasValue && (now - _lastLaunch.Value) <= TimeSpan.FromSeconds(2))
            {
                return;
            }

            _lastLaunch = now;
            var intent = new Intent(this, typeof(MainActivity));
            if (!callingIntent.Flags.HasFlag(ActivityFlags.LaunchedFromHistory))
            {
                intent.PutExtra("uri", _lastQueriedUri);
            }
            StartActivityForResult(intent, requestCode);
        }
    }
}
