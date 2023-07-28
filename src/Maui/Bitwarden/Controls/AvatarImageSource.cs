using System;
using System.IO;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Utilities;
using SkiaSharp;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public class AvatarImageSource : StreamImageSource
    {
        private readonly string _text;
        private readonly string _id;
        private readonly string _color;

        public override bool Equals(object obj)
        {
            if (obj is null)
            {
                return false;
            }

            if (obj is AvatarImageSource avatar)
            {
                return avatar._id == _id && avatar._text == _text && avatar._color == _color;
            }

            return base.Equals(obj);
        }

        public override int GetHashCode() => _id?.GetHashCode() ?? _text?.GetHashCode() ?? -1;

        public AvatarImageSource(string userId = null, string name = null, string email = null, string color = null)
        {
            _id = userId;
            _text = name;
            if (string.IsNullOrWhiteSpace(_text))
            {
                _text = email;
            }
            _color = color;
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
            string chars;
            string upperCaseText = null;

            if (string.IsNullOrEmpty(_text))
            {
                chars = "..";
            }
            else if (_text?.Length > 1)
            {
                upperCaseText = _text.ToUpper();
                chars = GetFirstLetters(upperCaseText, 2);
            }
            else
            {
                chars = upperCaseText = _text.ToUpper();
            }

            var bgColor = _color ?? CoreHelpers.StringToColor(_id ?? upperCaseText, "#33ffffff");
            var textColor = CoreHelpers.TextColorFromBgColor(bgColor);
            var size = 50;

            using (var bitmap = new SKBitmap(size * 2,
                size * 2,
                SKImageInfo.PlatformColorType,
                SKAlphaType.Premul))
            {
                using (var canvas = new SKCanvas(bitmap))
                {
                    canvas.Clear(SKColors.Transparent);
                    using (var paint = new SKPaint
                    {
                        IsAntialias = true,
                        Style = SKPaintStyle.Fill,
                        StrokeJoin = SKStrokeJoin.Miter,
                        Color = SKColor.Parse(bgColor)
                    })
                    {
                        var midX = canvas.LocalClipBounds.Size.ToSizeI().Width / 2;
                        var midY = canvas.LocalClipBounds.Size.ToSizeI().Height / 2;
                        var radius = midX - midX / 5;

                        using (var circlePaint = new SKPaint
                        {
                            IsAntialias = true,
                            Style = SKPaintStyle.Fill,
                            StrokeJoin = SKStrokeJoin.Miter,
                            Color = SKColor.Parse(bgColor)
                        })
                        {
                            canvas.DrawCircle(midX, midY, radius, circlePaint);

                            var typeface = SKTypeface.FromFamilyName("Arial", SKFontStyle.Normal);
                            var textSize = midX / 1.3f;
                            using (var textPaint = new SKPaint
                            {
                                IsAntialias = true,
                                Style = SKPaintStyle.Fill,
                                Color = SKColor.Parse(textColor),
                                TextSize = textSize,
                                TextAlign = SKTextAlign.Center,
                                Typeface = typeface
                            })
                            {
                                var rect = new SKRect();
                                textPaint.MeasureText(chars, ref rect);
                                canvas.DrawText(chars, midX, midY + rect.Height / 2, textPaint);

                                using (var img = SKImage.FromBitmap(bitmap))
                                {
                                    var data = img.Encode(SKEncodedImageFormat.Png, 100);
                                    return data?.AsStream(true);
                                }
                            }
                        }
                    }
                }
            }
        }

        private string GetFirstLetters(string data, int charCount)
        {
            var sanitizedData = data.Trim();
            var parts = sanitizedData.Split(new char[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);

            if (parts.Length > 1 && charCount <= 2)
            {
                var text = string.Empty;
                for (var i = 0; i < charCount; i++)
                {
                    text += parts[i][0];
                }
                return text;
            }
            if (sanitizedData.Length > 2)
            {
                return sanitizedData.Substring(0, 2);
            }
            return sanitizedData;
        }

        private Color StringToColor(string str)
        {
            if (str == null)
            {
                return Color.FromArgb("#33ffffff");
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
            return Color.FromArgb(color);
        }
    }
}
