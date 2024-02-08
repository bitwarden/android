using SkiaSharp;

namespace Bit.App.Controls
{
    public static class SKAvatarImageHelper
    {
        public static SKImage Draw(AvatarInfo avatarInfo)
        {
            using (var bitmap = new SKBitmap(avatarInfo.Size * 2,
                avatarInfo.Size * 2,
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
                        Color = SKColor.Parse(avatarInfo.BackgroundColor)
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
                            Color = SKColor.Parse(avatarInfo.BackgroundColor)
                        })
                        {
                            canvas.DrawCircle(midX, midY, radius, circlePaint);

                            var typeface = SKTypeface.FromFamilyName("Arial", SKFontStyle.Normal);
                            var textSize = midX / 1.3f;
                            using (var textPaint = new SKPaint
                            {
                                IsAntialias = true,
                                Style = SKPaintStyle.Fill,
                                Color = SKColor.Parse(avatarInfo.TextColor),
                                TextSize = textSize,
                                TextAlign = SKTextAlign.Center,
                                Typeface = typeface
                            })
                            {
                                var rect = new SKRect();
                                textPaint.MeasureText(avatarInfo.CharsToDraw, ref rect);
                                canvas.DrawText(avatarInfo.CharsToDraw, midX, midY + rect.Height / 2, textPaint);

                                return SKImage.FromBitmap(bitmap);
                            }
                        }
                    }
                }
            }
        }
    }
}
