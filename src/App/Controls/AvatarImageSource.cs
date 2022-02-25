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

        public override bool Equals(object obj)
        {
            if (obj is null)
            {
                return false;
            }

            if (obj is AvatarImageSource avatar)
            {
                return avatar._data == _data;
            }

            return base.Equals(obj);
        }

        public override int GetHashCode() => _data?.GetHashCode() ?? -1;

        public AvatarImageSource(string name = null, string email = null)
        {
            _data = name;
            if (string.IsNullOrWhiteSpace(_data))
            {
                _data = email;
            }
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
            else if (_data?.Length > 1)
            {
                upperData = _data.ToUpper();
                chars = GetFirstLetters(upperData, 2);
            }

            var bgColor = StringToColor(upperData);
            var textColor = Color.White;
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

        private string GetFirstLetters(string data, int charCount)
        {
            var parts  = data.Split();
            if (parts.Length > 1 && charCount <= 2)
            {
                var text = "";
                for (int i = 0; i < charCount; i++)
                {
                    text += parts[i].Substring(0,1);
                }
                return text;
            }
            if (data.Length > 2)
            {
                return data.Substring(0, 2);
            }
            return null;
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
                var base16 = "00" + Convert.ToString(value, 16);
                color += base16.Substring(base16.Length - 2);
            }
            return Color.FromHex(color);
        }
    }
}
