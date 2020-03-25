using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Service.QuickSettings;
using Bit.App.Resources;
using Bit.Droid.Accessibility;
using Java.Lang;

namespace Bit.Droid.Tile
{
    [Service(Permission = Android.Manifest.Permission.BindQuickSettingsTile, Label = "@string/ScanAndFill",
        Icon = "@drawable/shield")]
    [IntentFilter(new string[] { ActionQsTile })]
    [Register("com.x8bit.bitwarden.AutofillTileService")]
    public class AutofillTileService : TileService
    {
        public override void OnTileAdded()
        {
            base.OnTileAdded();
            AccessibilityHelpers.IsAutofillTileAdded = true;
        }

        public override void OnStartListening()
        {
            base.OnStartListening();
        }

        public override void OnStopListening()
        {
            base.OnStopListening();
        }

        public override void OnTileRemoved()
        {
            base.OnTileRemoved();
            AccessibilityHelpers.IsAutofillTileAdded = false;
        }

        public override void OnClick()
        {
            base.OnClick();
            
            if(IsLocked)
            {
                UnlockAndRun(new Runnable(ScanAndFill));
            }
            else
            {
                ScanAndFill();
            }
        }

        private void ScanAndFill()
        {
            if(!AccessibilityHelpers.IsAccessibilityBroadcastReady)
            {
                ShowConfigErrorDialog();
                return;
            }
            
            var intent = new Intent(this, typeof(AccessibilityActivity));
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
            intent.PutExtra("autofillTileClicked", true);
            StartActivityAndCollapse(intent);
        }

        private void ShowConfigErrorDialog()
        {
            var alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.SetMessage(AppResources.AutofillTileAccessibilityRequired);
            alertBuilder.SetCancelable(true);
            alertBuilder.SetPositiveButton(AppResources.Ok, (sender, args) =>
            {
                (sender as AlertDialog)?.Cancel();
            });
            ShowDialog(alertBuilder.Create());
        }
    }
}
