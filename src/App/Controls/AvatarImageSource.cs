using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using SkiaSharp;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class AvatarImageSource : StreamImageSource
    {
        private string _data;
        
        public AvatarImageSource(string data = null)
        {
            _data = data;
        }
        
        public override Func<CancellationToken, Task<Stream>> Stream => GetStreamAsync;

        private Task<Stream> GetStreamAsync(CancellationToken userToken = new CancellationToken())
        {
            OnLoadingStarted();
            userToken.Register(CancellationTokenSource.Cancel);
            var result = Draw();
            OnLoadingCompleted(CancellationTokenSource.IsCancellationRequested);
            return Task.FromResult(result);
        }

        private Stream Draw()
        {
            string chars = null;
            string upperData = null;
            
            if (string.IsNullOrEmpty(_data))
            {
                chars = "..";
            }
            else if (_data?.Length > 2)
            {
                upperData = _data.ToUpper();
                chars = upperData.Substring(0, 2).ToUpper();
            }
            
            var bgColor = StringToColor(upperData);
            var textColor = Color.White;;
            var size = 50;
            
            var bitmap = new SKBitmap(
                size * 2,
                size * 2,
                SKImageInfo.PlatformColorType,
                SKAlphaType.Premul);
            var canvas = new SKCanvas(bitmap);
            canvas.Clear(SKColors.Transparent);

            var midX = canvas.LocalClipBounds.Size.ToSizeI().Width / 2;
            var midY = canvas.LocalClipBounds.Size.ToSizeI().Height / 2;
            var radius = midX - midX / 5;

            var circlePaint = new SKPaint
            {
                IsAntialias = true,
                Style = SKPaintStyle.Fill,
                StrokeJoin = SKStrokeJoin.Miter,
                Color = SKColor.Parse(bgColor.ToHex())
            };
            canvas.DrawCircle(midX, midY, radius, circlePaint);

            var typeface = SKTypeface.FromFamilyName("Arial", SKFontStyle.Normal);
            var textSize = midX / 1.3f;
            var textPaint = new SKPaint
            {
                IsAntialias = true,
                Style = SKPaintStyle.Fill,
                Color = SKColor.Parse(textColor.ToHex()),
                TextSize = textSize,
                TextAlign = SKTextAlign.Center,
                Typeface = typeface
            };
            var rect = new SKRect();
            textPaint.MeasureText(chars, ref rect);
            canvas.DrawText(chars, midX, midY + rect.Height / 2, textPaint);

            return SKImage.FromBitmap(bitmap).Encode(SKEncodedImageFormat.Png, 100).AsStream();
        }
        
        private Color StringToColor(string str)
        {
            if (str == null)
            {
                return Color.FromHex("#33ffffff");
            }
            var hash = 0;
            for (var i = 0; i < str.Length; i++)
            {
                hash = str[i] + ((hash << 5) - hash);
            }
            var color = "#FF";
            for (var i = 0; i < 3; i++)
            {
                var value = (hash >> (i * 8)) & 0xff;
                color += Convert.ToString(value, 16);
            }
            if (Device.RuntimePlatform == Device.iOS)
            {
                // TODO remove this once iOS ToolbarItem tint issue is solved
                return Color.FromHex("#33ffffff");
            }
            return Color.FromHex(color);
        }
    }
}
