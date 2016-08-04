using System;
using UIKit;
using Bit.App.Abstractions;
using XLabs.Ioc;

namespace Bit.iOS.Core.Controllers
{
    public class ExtendedUIViewController : UIViewController
    {
        public ExtendedUIViewController(IntPtr handle)
            : base(handle)
        { }

        public override void ViewDidAppear(bool animated)
        {
            var googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            googleAnalyticsService.TrackPage(GetType().Name);
            base.ViewDidAppear(animated);
        }
    }
}
