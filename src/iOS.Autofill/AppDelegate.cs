using System;
using Foundation;
using Microsoft.Maui;
using Microsoft.Maui.Embedding;
using Microsoft.Maui.Hosting;
using UIKit;

namespace Bit.iOS.Autofill
{
    public class MauiContextSingleton
    {
        private static Lazy<MauiContextSingleton> _instance = new Lazy<MauiContextSingleton>(() => new MauiContextSingleton());

        private MauiContextSingleton() { }

        public static MauiContextSingleton Instance = _instance.Value;

        public MauiContext MauiContext { get; set; }
    }

    [Register("AppDelegate")]
    public partial class AppDelegate : UIApplicationDelegate
    {
        public override UIWindow Window
        {
            get; set;
        }

        public override void OnResignActivation(UIApplication application)
        {
        }

        public override void DidEnterBackground(UIApplication application)
        {
        }

        public override void WillEnterForeground(UIApplication application)
        {
        }

        public override void WillTerminate(UIApplication application)
        {
        }

        public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
        {
            var builder = MauiApp.CreateBuilder();
            builder.UseMauiEmbedding<Microsoft.Maui.Controls.Application>();
            // Register the Window
            builder.Services.Add(new Microsoft.Extensions.DependencyInjection.ServiceDescriptor(typeof(UIWindow), Window));
            var mauiApp = builder.Build();
            MauiContextSingleton.Instance.MauiContext = new MauiContext(mauiApp.Services);

            return base.FinishedLaunching(application, launchOptions);
        }
    }
}
