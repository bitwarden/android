using Bit.App.Controls;
using Bit.App.Resources;
using System;
using System.Collections.Generic;
using Xamarin.Forms;
using ZXing.Net.Mobile.Forms;

namespace Bit.App.Pages
{
    public class ScanPage : ExtendedContentPage
    {
        private readonly ZXingScannerView _zxing;
        private readonly OverlayGrid _overlay;
        private bool _pageDisappeared = true;

        public ScanPage(Action<string> callback)
            : base(updateActivity: false)
        {
            _zxing = new ZXingScannerView
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                VerticalOptions = LayoutOptions.FillAndExpand,
                AutomationId = "zxingScannerView",
                Options = new ZXing.Mobile.MobileBarcodeScanningOptions
                {
                    UseNativeScanning = true,
                    PossibleFormats = new List<ZXing.BarcodeFormat> { ZXing.BarcodeFormat.QR_CODE },
                    AutoRotate = false
                }
            };

            _zxing.OnScanResult += (result) =>
            {
                // Stop analysis until we navigate away so we don't keep reading barcodes
                _zxing.IsAnalyzing = false;
                _zxing.IsScanning = false;

                Uri uri;
                if(!string.IsNullOrWhiteSpace(result.Text) && Uri.TryCreate(result.Text, UriKind.Absolute, out uri) &&
                    !string.IsNullOrWhiteSpace(uri.Query))
                {
                    var queryParts = uri.Query.Substring(1).ToLowerInvariant().Split('&');
                    foreach(var part in queryParts)
                    {
                        if(part.StartsWith("secret="))
                        {
                            callback(part.Substring(7)?.ToUpperInvariant());
                            return;
                        }
                    }
                }

                callback(null);
            };

            _overlay = new OverlayGrid
            {
                AutomationId = "zxingDefaultOverlay"
            };

            _overlay.TopLabel.Text = AppResources.CameraInstructionTop;
            _overlay.BottomLabel.Text = AppResources.CameraInstructionBottom;

            var grid = new Grid
            {
                VerticalOptions = LayoutOptions.FillAndExpand,
                HorizontalOptions = LayoutOptions.FillAndExpand,
                Children = { _zxing, _overlay }
            };

            if(Device.RuntimePlatform == Device.iOS || Device.RuntimePlatform == Device.Windows)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.ScanQrTitle;
            Content = grid;
        }

        protected override void OnAppearing()
        {
            _pageDisappeared = false;
            base.OnAppearing();
            _zxing.IsScanning = true;
            Device.StartTimer(new TimeSpan(0, 0, 2), () =>
            {
                if(_pageDisappeared)
                {
                    return false;
                }

                _zxing.AutoFocus();
                return true;
            });
        }

        protected override void OnDisappearing()
        {
            _pageDisappeared = true;
            _zxing.IsScanning = false;
            base.OnDisappearing();
        }

        public class OverlayGrid : Grid
        {
            public OverlayGrid()
            {
                VerticalOptions = LayoutOptions.FillAndExpand;
                HorizontalOptions = LayoutOptions.FillAndExpand;

                RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
                RowDefinitions.Add(new RowDefinition { Height = new GridLength(2, GridUnitType.Star) });
                RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });

                Children.Add(new BoxView
                {
                    VerticalOptions = LayoutOptions.Fill,
                    HorizontalOptions = LayoutOptions.FillAndExpand,
                    BackgroundColor = Color.Black,
                    Opacity = 0.7,
                }, 0, 0);

                Children.Add(new BoxView
                {
                    VerticalOptions = LayoutOptions.Center,
                    HorizontalOptions = LayoutOptions.FillAndExpand,
                    BackgroundColor = Color.Transparent
                }, 0, 1);

                Children.Add(new BoxView
                {
                    VerticalOptions = LayoutOptions.Fill,
                    HorizontalOptions = LayoutOptions.FillAndExpand,
                    BackgroundColor = Color.Black,
                    Opacity = 0.7,
                }, 0, 2);

                TopLabel = new Label
                {
                    VerticalOptions = LayoutOptions.Center,
                    HorizontalOptions = LayoutOptions.Center,
                    TextColor = Color.White,
                    AutomationId = "zxingDefaultOverlay_TopTextLabel",
                };
                Children.Add(TopLabel, 0, 0);

                BottomLabel = new Label
                {
                    VerticalOptions = LayoutOptions.Center,
                    HorizontalOptions = LayoutOptions.Center,
                    TextColor = Color.White,
                    AutomationId = "zxingDefaultOverlay_BottomTextLabel",
                };
                Children.Add(BottomLabel, 0, 2);
            }

            public Label TopLabel { get; set; }
            public Label BottomLabel { get; set; }
        }
    }
}
