using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using SkiaSharp;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using SkiaSharp.Views.Maui.Controls;
using SkiaSharp.Views.Maui;
using ZXing.Net.Maui;
using Kotlin.Coroutines.Jvm.Internal;

namespace Bit.App.Pages
{
    public partial class ScanPage : BaseContentPage
    {
        private ScanPageViewModel ViewModel => BindingContext as ScanPageViewModel;
        private readonly Action<string> _callback;
        private CancellationTokenSource _autofocusCts;
        private Task _continuousAutofocusTask;
        private readonly Color _greenColor;
        private readonly SKColor _blueSKColor;
        private readonly SKColor _greenSKColor;
        private readonly Stopwatch _stopwatch;
        private bool _pageIsActive;
        private bool _qrcodeFound;
        private float _scale;
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        public ScanPage(Action<string> callback)
        {
            InitializeComponent();
            _callback = callback;
            ViewModel.InitScannerCommand = new Command(() => InitScanner());

            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }

            _greenColor = ThemeManager.GetResourceColor("SuccessColor");
            _greenSKColor = _greenColor.ToSKColor();
            _blueSKColor = ThemeManager.GetResourceColor("PrimaryColor").ToSKColor();
            _stopwatch = new Stopwatch();
            _qrcodeFound = false;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            StartScanner();
        }

        protected override void OnDisappearing()
        {
            StopScanner().FireAndForget();
            base.OnDisappearing();
        }

        // Fix known bug with DelayBetweenAnalyzingFrames & DelayBetweenContinuousScans: https://github.com/Redth/ZXing.Net.Mobile/issues/721
        private void InitScanner()
        {
            try
            {
                if (!ViewModel.HasCameraPermission || !ViewModel.ShowScanner || _zxing != null)
                {
                    return;
                }

                //_zxing = new ZXingScannerView();
                _zxing.Options = new BarcodeReaderOptions
                {
                    //UseNativeScanning = true,
                    //PossibleFormats = new List<ZXing.BarcodeFormat> { ZXing.BarcodeFormat.QR_CODE },
                    Formats = BarcodeFormat.QrCode,
                    AutoRotate = false,
                    TryInverted = true,
                    //DelayBetweenAnalyzingFrames = 5,
                    //DelayBetweenContinuousScans = 5
                };
                //_scannerContainer.Content = _zxing;
                StartScanner();
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }

        private void StartScanner()
        {
            if (_zxing == null)
            {
                return;
            }

            //_zxing.OnScanResult -= OnScanResult;
            //_zxing.OnScanResult += OnScanResult;
            // TODO: [MAUI-Migration] [Critical]
            //_zxing.IsScanning = true;

            // Fix for Autofocus, now it's done every 2 seconds so that the user does't have to do it
            // https://github.com/Redth/ZXing.Net.Mobile/issues/414
            _autofocusCts?.Cancel();
            _autofocusCts = new CancellationTokenSource(TimeSpan.FromMinutes(3));

            var autofocusCts = _autofocusCts;
            // this task is needed to be awaited OnDisappearing to avoid some crashes
            // when changing the value of _zxing.IsScanning
            _continuousAutofocusTask = Task.Run(async () =>
            {
                try
                {
                    while (!autofocusCts.IsCancellationRequested)
                    {
                        await Task.Delay(TimeSpan.FromSeconds(2), autofocusCts.Token);
                        await Device.InvokeOnMainThreadAsync(() =>
                        {
                            if (!autofocusCts.IsCancellationRequested)
                            {
                                try
                                {
                                    _zxing.AutoFocus();
                                }
                                catch (Exception ex)
                                {
                                    _logger.Value.Exception(ex);
                                }
                            }
                        });
                    }
                }
                catch (TaskCanceledException) { }
                catch (Exception ex)
                {
                    _logger.Value.Exception(ex);
                }
            }, autofocusCts.Token);
            _pageIsActive = true;
            AnimationLoopAsync();
        }

        private async Task StopScanner()
        {
            if (_zxing == null)
            {
                return;
            }

            _autofocusCts?.Cancel();
            if (_continuousAutofocusTask != null)
            {
                await _continuousAutofocusTask;
            }
            // TODO: [MAUI-Migration] [Critical]
            //_zxing.IsScanning = false;
            //_zxing.OnScanResult -= OnScanResult;
            _pageIsActive = false;
        }

        // TODO: [MAUI-Migration] [Critical]
        private async void _zxing_BarcodesDetected(System.Object sender, ZXing.Net.Maui.BarcodeDetectionEventArgs e)
        {
            try
            {
                if (!e.Results.Any())
                {
                    return;
                }
                var result = e.Results[0];

                // Stop analysis until we navigate away so we don't keep reading barcodes
                // Stop analysis until we navigate away so we don't keep reading barcodes
                // TODO: [MAUI-Migration] [Critical]
                //_zxing.IsAnalyzing = false;
                var text = result?.Value;
                if (!string.IsNullOrWhiteSpace(text))
                {
                    if (text.StartsWith("otpauth://totp"))
                    {
                        await QrCodeFoundAsync();
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
                                await QrCodeFoundAsync();
                                var subResult = part.Substring(7);
                                if (!string.IsNullOrEmpty(subResult))
                                {
                                    _callback(subResult.ToUpperInvariant());
                                }
                                return;
                            }
                        }
                    }
                }
                _callback(null);
            }
            catch (Exception ex)
            {
                _logger?.Value?.Exception(ex);
            }
        }

        private async Task QrCodeFoundAsync()
        {
            _qrcodeFound = true;
            Vibration.Vibrate();
            await Task.Delay(1000);
            // TODO: [MAUI-Migration] [Critical]
            //_zxing.IsScanning = false;
        }

        private async void Close_Clicked(object sender, System.EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }

        private void AddAuthenticationKey_OnClicked(object sender, EventArgs e)
        {
            if (!string.IsNullOrWhiteSpace(ViewModel.TotpAuthenticationKey))
            {
                _callback(ViewModel.TotpAuthenticationKey);
                return;
            }
            _callback(null);
        }

        private void ToggleScanMode_OnTapped(object sender, EventArgs e)
        {
            ViewModel.ToggleScanModeCommand.Execute(null);
            if (!ViewModel.ShowScanner)
            {
                _authenticationKeyEntry.Focus();
            }
        }

        private void OnCanvasViewPaintSurface(object sender, SKPaintSurfaceEventArgs args)
        {
            var info = args.Info;
            var surface = args.Surface;
            var canvas = surface.Canvas;
            var margins = 20;
            var maxSquareSize = (Math.Min(info.Height, info.Width) * 0.9f - margins) * _scale;
            var squareSize = maxSquareSize;
            var lineSize = squareSize * 0.15f;
            var startXPoint = (info.Width / 2) - (squareSize / 2);
            var startYPoint = (info.Height / 2) - (squareSize / 2);
            canvas.Clear(SKColors.Transparent);

            using (var strokePaint = new SKPaint
            {
                Color = _qrcodeFound ? _greenSKColor : _blueSKColor,
                StrokeWidth = 9 * _scale,
                StrokeCap = SKStrokeCap.Round,
            })
            {
                canvas.Scale(1, 1);
                //top left
                canvas.DrawLine(startXPoint, startYPoint, startXPoint, startYPoint + lineSize, strokePaint);
                canvas.DrawLine(startXPoint, startYPoint, startXPoint + lineSize, startYPoint, strokePaint);
                //bot left
                canvas.DrawLine(startXPoint, startYPoint + squareSize, startXPoint, startYPoint + squareSize - lineSize, strokePaint);
                canvas.DrawLine(startXPoint, startYPoint + squareSize, startXPoint + lineSize, startYPoint + squareSize, strokePaint);
                //top right
                canvas.DrawLine(startXPoint + squareSize, startYPoint, startXPoint + squareSize - lineSize, startYPoint, strokePaint);
                canvas.DrawLine(startXPoint + squareSize, startYPoint, startXPoint + squareSize, startYPoint + lineSize, strokePaint);
                //bot right
                canvas.DrawLine(startXPoint + squareSize, startYPoint + squareSize, startXPoint + squareSize - lineSize, startYPoint + squareSize, strokePaint);
                canvas.DrawLine(startXPoint + squareSize, startYPoint + squareSize, startXPoint + squareSize, startYPoint + squareSize - lineSize, strokePaint);
            }
        }

        async Task AnimationLoopAsync()
        {
            try
            {
                _stopwatch.Start();
                while (_pageIsActive)
                {
                    var t = _stopwatch.Elapsed.TotalSeconds % 2 / 2;
                    _scale = (20 - (1 - (float)Math.Sin(4 * Math.PI * t))) / 20;
                    SkCanvasView.InvalidateSurface();
                    await Task.Delay(TimeSpan.FromSeconds(1.0 / 30));
                    if (_qrcodeFound && _scale > 0.98f)
                    {
                        _checkIcon.TextColor = _greenColor;
                        SkCanvasView.InvalidateSurface();
                        break;
                    }
                }
            }
            catch (Exception ex)
            {
                _logger?.Value?.Exception(ex);
            }
            finally
            {
                _stopwatch?.Stop();
            }
        }
    }
}
