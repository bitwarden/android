using System;
using System.Runtime.CompilerServices;
using SkiaSharp;
using SkiaSharp.Views.Forms;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class CircularProgressbarView : SKCanvasView
    {
        private Circle _circle;

        public static readonly BindableProperty ProgressProperty = BindableProperty.Create(
            nameof(Progress), typeof(double), typeof(CircularProgressbarView), propertyChanged: OnProgressChanged);

        public static readonly BindableProperty RadiusProperty = BindableProperty.Create(
            nameof(Radius), typeof(float), typeof(CircularProgressbarView), 15f);

        public static readonly BindableProperty StrokeWidthProperty = BindableProperty.Create(
            nameof(StrokeWidth), typeof(float), typeof(CircularProgressbarView), 3f);

        public static readonly BindableProperty ProgressColorProperty = BindableProperty.Create(
            nameof(ProgressColor), typeof(Color), typeof(CircularProgressbarView), Color.FromHex("175DDC"));

        public static readonly BindableProperty EndingProgressColorProperty = BindableProperty.Create(
            nameof(EndingProgressColor), typeof(Color), typeof(CircularProgressbarView), Color.FromHex("dd4b39"));

        public static readonly BindableProperty BackgroundProgressColorProperty = BindableProperty.Create(
            nameof(BackgroundProgressColor), typeof(Color), typeof(CircularProgressbarView), Color.White);

        public double Progress
        {
            get { return (double)GetValue(ProgressProperty); }
            set { SetValue(ProgressProperty, value); }
        }

        public float Radius
        {
            get => (float)GetValue(RadiusProperty);
            set => SetValue(RadiusProperty, value);
        }
        public float StrokeWidth
        {
            get => (float)GetValue(StrokeWidthProperty);
            set => SetValue(StrokeWidthProperty, value);
        }

        public Color ProgressColor
        {
            get => (Color)GetValue(ProgressColorProperty);
            set => SetValue(ProgressColorProperty, value);
        }

        public Color EndingProgressColor
        {
            get => (Color)GetValue(EndingProgressColorProperty);
            set => SetValue(EndingProgressColorProperty, value);
        }

        public Color BackgroundProgressColor
        {
            get => (Color)GetValue(BackgroundProgressColorProperty);
            set => SetValue(BackgroundProgressColorProperty, value);
        }

        private static void OnProgressChanged(BindableObject bindable, object oldvalue, object newvalue)
        {
            var context = bindable as CircularProgressbarView;
            context.InvalidateSurface();
        }

        protected override void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == nameof(Progress))
            {
                _circle = new Circle(Radius * (float)DeviceDisplay.MainDisplayInfo.Density, (info) => new SKPoint((float)info.Width / 2, (float)info.Height / 2));
            }
        }

        protected override void OnPaintSurface(SKPaintSurfaceEventArgs e)
        {
            base.OnPaintSurface(e);
            if (_circle != null)
            {
                _circle.CalculateCenter(e.Info);
                e.Surface.Canvas.Clear();
                DrawCircle(e.Surface.Canvas, _circle, StrokeWidth * (float)DeviceDisplay.MainDisplayInfo.Density, BackgroundProgressColor.ToSKColor());
                DrawArc(e.Surface.Canvas, _circle, () => (float)Progress, StrokeWidth * (float)DeviceDisplay.MainDisplayInfo.Density, ProgressColor.ToSKColor(), EndingProgressColor.ToSKColor());
            }
        }

        private void DrawCircle(SKCanvas canvas, Circle circle, float strokewidth, SKColor color)
        {
            canvas.DrawCircle(circle.Center, circle.Radius,
                new SKPaint()
                {
                    StrokeWidth = strokewidth,
                    Color = color,
                    IsStroke = true,
                    IsAntialias = true
                });
        }

        private void DrawArc(SKCanvas canvas, Circle circle, Func<float> progress, float strokewidth, SKColor color, SKColor progressEndColor)
        {
            var progressValue = progress();
            var angle = progressValue * 3.6f;
            canvas.DrawArc(circle.Rect, 270, angle, false,
                new SKPaint()
                {
                    StrokeWidth = strokewidth,
                    Color = progressValue < 20f ? progressEndColor : color,
                    IsStroke = true,
                    IsAntialias = true
                });
        }
    }

    public class Circle
    {
        private readonly Func<SKImageInfo, SKPoint> _centerFunc;

        public Circle(float radius, Func<SKImageInfo, SKPoint> centerFunc)
        {
            _centerFunc = centerFunc;
            Radius = radius;
        }
        public SKPoint Center { get; set; }
        public float Radius { get; set; }
        public SKRect Rect => new SKRect(Center.X - Radius, Center.Y - Radius, Center.X + Radius, Center.Y + Radius);

        public void CalculateCenter(SKImageInfo argsInfo)
        {
            Center = _centerFunc(argsInfo);
        }
    }
}
