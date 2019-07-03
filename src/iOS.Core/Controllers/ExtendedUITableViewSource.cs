using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public abstract class ExtendedUITableViewSource : UITableViewSource
    {
        public override void WillDisplayHeaderView(UITableView tableView, UIView headerView, nint section)
        {
            if(headerView != null && headerView is UITableViewHeaderFooterView hv)
            {
                hv.BackgroundColor = ThemeHelpers.ListHeaderBackgroundColor;
                hv.TintColor = ThemeHelpers.ListHeaderBackgroundColor;
                if(hv.BackgroundView != null)
                {
                    hv.BackgroundView.BackgroundColor = ThemeHelpers.ListHeaderBackgroundColor;
                }
                if(hv.TextLabel != null)
                {
                    hv.TextLabel.TextColor = ThemeHelpers.MutedColor;
                }
            }
        }

        public override void WillDisplayFooterView(UITableView tableView, UIView footerView, nint section)
        {
            if(footerView != null && footerView is UITableViewHeaderFooterView fv)
            {
                fv.BackgroundColor = ThemeHelpers.ListHeaderBackgroundColor;
                fv.TintColor = ThemeHelpers.ListHeaderBackgroundColor;
                if(fv.BackgroundView != null)
                {
                    fv.BackgroundView.BackgroundColor = ThemeHelpers.ListHeaderBackgroundColor;
                }
                if(fv.TextLabel != null)
                {
                    fv.TextLabel.TextColor = ThemeHelpers.MutedColor;
                }
            }
        }
    }
}
