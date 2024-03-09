using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Service.QuickSettings;
using Java.Lang;

namespace Bit.Droid.Tile
{
    [Service(Permission = Android.Manifest.Permission.BindQuickSettingsTile, Label = "@string/MyVault",
        Icon = "@drawable/shield",
        Exported = true)]
    [IntentFilter(new string[] { ActionQsTile })]
    [Register("com.x8bit.bitwarden.MyVaultTileService")]
    public class MyVaultTileService : TileService
    {
        public override void OnTileAdded()
        {
            base.OnTileAdded();
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
        }

        public override void OnClick()
        {
            base.OnClick();

            if (IsLocked)
            {
                UnlockAndRun(new Runnable(() =>
                {
                    LaunchMyVault();
                }));
            }
            else
            {
                LaunchMyVault();
            }
        }

        private void LaunchMyVault()
        {
            var intent = new Intent(this, typeof(MainActivity));
            intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.SingleTop | ActivityFlags.ClearTop);
            intent.PutExtra("myVaultTile", true);

            //For Android 14+ We need to use PendingIntent instead of Intent directly. Older versions still need to use Intent.
            if (Build.VERSION.SdkInt >= BuildVersionCodes.UpsideDownCake)
            {
                var pendingIntent = PendingIntent.GetActivity(
                    ApplicationContext,
                    0,
                    intent,
                    PendingIntentFlags.Immutable | PendingIntentFlags.UpdateCurrent
                );
                if (pendingIntent == null) { return; }

                StartActivityAndCollapse(pendingIntent);
            }
            else
            {
                StartActivityAndCollapse(intent);
            }
        }
    }
}
