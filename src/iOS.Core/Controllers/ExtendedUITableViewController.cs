using Bit.App.Abstractions;
using System;
using UIKit;
using XLabs.Ioc;

namespace Bit.iOS.Core.Controllers
{
    public class ExtendedUITableViewController : UITableViewController
    {
        public ExtendedUITableViewController(IntPtr handle)
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
