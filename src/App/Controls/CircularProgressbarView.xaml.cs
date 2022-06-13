using System;
using System.Collections.Generic;
using SkiaSharp;
using SkiaSharp.Views.Forms;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class CircularProgressbarView : ContentView
    {
        private ProgressDrawer _progressDrawer;

        public static readonly BindableProperty ProgressProperty = BindableProperty.Create(
            "Progress", typeof(double), typeof(CircularProgressbarView), propertyChanged: OnProgressChanged);

        public SKColor Color => Progress > 90 ? SKColors.Red : SKColors.Blue;

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
            var circle = new Circle(25, (info) => new SKPoint((float)info.Width / 2, (float)info.Height / 2));
            _progressDrawer = new ProgressDrawer(SkCanvasView, circle, () => (float)Progress, 5, SKColors.White, Color);
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

        public class ProgressDrawer
        {

            public ProgressDrawer(SKCanvasView canvas, Circle circle, Func<float> progress, float strokeWidth, SKColor progressColor, SKColor foregroundColor)
            {
                canvas.PaintSurface += (sender, args) =>
                {
                    circle.CalculateCenter(args.Info);
                    args.Surface.Canvas.Clear();
                    DrawCircle(args.Surface.Canvas, circle, strokeWidth, progressColor);
                    DrawArc(args.Surface.Canvas, circle, progress, strokeWidth, foregroundColor);

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
                        IsAntialias = true,
                    });

            }

            private void DrawArc(SKCanvas canvas, Circle circle, Func<float> progress, float strokewidth, SKColor color)
            {
                var angle = progress.Invoke() * 3.6f;
                canvas.DrawArc(circle.Rect, 270, angle, false,
                    new SKPaint() { StrokeWidth = strokewidth, Color = color, IsStroke = true, IsAntialias = true });
            }

        }

        public void CalculateCenter(SKImageInfo argsInfo)
        {
            Center = _centerfunc.Invoke(argsInfo);
        }
    }
    public class ProgressDrawer
    {
        public ProgressDrawer(SKCanvasView canvas, Circle circle, Func<float> progress, float strokeWidth, SKColor progressColor, SKColor foregroundColor)
        {
            canvas.PaintSurface += (sender, args) =>
            {
                circle.CalculateCenter(args.Info);
                args.Surface.Canvas.Clear();
                DrawCircle(args.Surface.Canvas, circle, strokeWidth, progressColor);
                DrawArc(args.Surface.Canvas, circle, progress, strokeWidth, foregroundColor);

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

        private void DrawArc(SKCanvas canvas, Circle circle, Func<float> progress, float strokewidth, SKColor color)
        {
            var angle = progress.Invoke() * 3.6f;
            canvas.DrawArc(circle.Rect, 270, angle, false,
                new SKPaint() { StrokeWidth = strokewidth, Color = color, IsStroke = true,
                    IsAntialias = true
                });
        }
    }
}
