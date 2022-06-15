using System;
using System.Collections.Generic;
using SkiaSharp;
using SkiaSharp.Views.Forms;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class CircularProgressbarView : ContentView
    {
        private ProgressDrawer _progressDrawer;

        public static readonly BindableProperty ProgressProperty = BindableProperty.Create(
            "Progress", typeof(double), typeof(CircularProgressbarView), propertyChanged: OnProgressChanged);

        public double Progress
        {
            get { return (double)GetValue(ProgressProperty); }
            set { SetValue(ProgressProperty, value); }
        }

        private static void OnProgressChanged(BindableObject bindable, object oldvalue, object newvalue)
        {
            var context = bindable as CircularProgressbarView;
            context.SkCanvasView.InvalidateSurface();
        }

        public CircularProgressbarView()
        {
            InitializeComponent();
            var pixels = DeviceDisplay.MainDisplayInfo.Density * 15;
            var circle = new Circle((float)pixels, (info) => new SKPoint((float)info.Width / 2, (float)info.Height / 2));
            _progressDrawer = new ProgressDrawer(SkCanvasView, circle, () => (float)Progress, 5, SKColors.White, SKColors.Blue, SKColors.Red);
        }
    }

    public class Circle
    {
        private readonly Func<SKImageInfo, SKPoint> _centerfunc;

        public Circle(float redius, Func<SKImageInfo, SKPoint> centerfunc)
        {
            _centerfunc = centerfunc;
            Redius = redius;
        }
        public SKPoint Center { get; set; }
        public float Redius { get; set; }
        public SKRect Rect => new SKRect(Center.X - Redius, Center.Y - Redius, Center.X + Redius, Center.Y + Redius);

        public void CalculateCenter(SKImageInfo argsInfo)
        {
            Center = _centerfunc.Invoke(argsInfo);
        }
    }

    public class ProgressDrawer
    {
        public ProgressDrawer(SKCanvasView canvas, Circle circle, Func<float> progress, float strokeWidth, SKColor progressColor, SKColor foregroundColor, SKColor progressEndColor)
        {
            canvas.PaintSurface += (sender, args) =>
            {
                circle.CalculateCenter(args.Info);
                args.Surface.Canvas.Clear();
                DrawCircle(args.Surface.Canvas, circle, strokeWidth, progressColor);
                DrawArc(args.Surface.Canvas, circle, progress, strokeWidth, foregroundColor, progressEndColor);
            };
        }

        private void DrawCircle(SKCanvas canvas, Circle circle, float strokewidth, SKColor color)
        {
            canvas.DrawCircle(circle.Center, circle.Redius,
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
            var progressValue = progress.Invoke();
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
}
