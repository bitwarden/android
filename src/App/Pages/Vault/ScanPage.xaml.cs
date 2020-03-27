using System;
using System.Collections.Generic;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class ScanPage : BaseContentPage
    {
        private readonly Action<string> _callback;

        private DateTime? _timerStarted = null;
        private TimeSpan _timerMaxLength = TimeSpan.FromMinutes(3);

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

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _zxing.IsScanning = true;
            _timerStarted = DateTime.Now;
            Device.StartTimer(new TimeSpan(0, 0, 2), () =>
            {
                if (_timerStarted == null || (DateTime.Now - _timerStarted) > _timerMaxLength)
                {
                    return false;
                }
                _zxing.AutoFocus();
                return true;
            });
        }

        protected override void OnDisappearing()
        {
            _timerStarted = null;
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
