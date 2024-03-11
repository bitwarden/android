using Android.App;
using Android.Content;
using Android.Runtime;
using Android.Service.QuickSettings;
using Bit.App.Droid.Utilities;
using Java.Lang;

namespace Bit.Droid.Tile
{
    [Service(Permission = Android.Manifest.Permission.BindQuickSettingsTile, Exported = true, Label = "@string/PasswordGenerator",
        Icon = "@drawable/generate")]
    [IntentFilter(new string[] { ActionQsTile })]
    [Register("com.x8bit.bitwarden.GeneratorTileService")]
    public class GeneratorTileService : TileService
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
            intent.PutExtra("generatorTile", true);
            this.StartActivityAndCollapseWithIntent(intent, isMutable: false);
        }
    }
}
