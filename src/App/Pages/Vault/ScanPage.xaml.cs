using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AppCenter.Crashes;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class ScanPage : BaseContentPage
    {
        private readonly Action<string> _callback;

        public ScanPage(Action<string> callback)
        {
            _callback = callback;
            InitializeComponent();
            _zxing.Options = new ZXing.Mobile.MobileBarcodeScanningOptions
            {
                UseNativeScanning = true,
                PossibleFormats = new List<ZXing.BarcodeFormat> { ZXing.BarcodeFormat.QR_CODE },
                AutoRotate = false,
                TryInverted = true
            };
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }
        }
        private CancellationTokenSource _autofocusCts;

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _zxing.IsScanning = true;

            // Fix for Autofocus, now it's done every 2 seconds so that the user does't have to do it
            // https://github.com/Redth/ZXing.Net.Mobile/issues/414
            _autofocusCts?.Cancel();
            _autofocusCts = new CancellationTokenSource();

            var autofocusCts = _autofocusCts;

            Task.Run(async () =>
            {
                await Task.Delay(TimeSpan.FromMinutes(3), autofocusCts.Token);
                autofocusCts.Cancel();
            });

            Device.StartTimer(TimeSpan.FromSeconds(2), () =>
            {
                try
                {
                    if (autofocusCts.IsCancellationRequested)
                    {
                        return false;
                    }

                    _zxing.AutoFocus();
                    return true;
                }
                catch (Exception ex)
                {
                    // we don't need to display anything to the user because at the most they just lose autofocus
#if !FDROID
                    Crashes.TrackError(ex);
#endif
                    autofocusCts?.Cancel(); // we also cancel here to cancel the Task.Delay as well.
                    return false;
                }
            });
        }

        protected override void OnDisappearing()
        {
            _autofocusCts?.Cancel();

            _zxing.IsScanning = false;
            base.OnDisappearing();
        }

        private void OnScanResult(ZXing.Result result)
        {
            // Stop analysis until we navigate away so we don't keep reading barcodes
            _zxing.IsAnalyzing = false;
            _zxing.IsScanning = false;
            var text = result?.Text;
            if (!string.IsNullOrWhiteSpace(text))
            {
                if (text.StartsWith("otpauth://totp"))
                {
                    _callback(text);
                    return;
                }
                else if (Uri.TryCreate(text, UriKind.Absolute, out Uri uri) &&
                    !string.IsNullOrWhiteSpace(uri?.Query))
                {
                    var queryParts = uri.Query.Substring(1).ToLowerInvariant().Split('&');
                    foreach (var part in queryParts)
                    {
                        if (part.StartsWith("secret="))
                        {
                            _callback(part.Substring(7)?.ToUpperInvariant());
                            return;
                        }
                    }
                }
            }
            _callback(null);
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
