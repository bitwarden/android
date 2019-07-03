using Bit.iOS.Core.Utilities;
using System;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public abstract class ExtendedUITableViewSource : UITableViewSource
    {
        public override void WillDisplayHeaderView(UITableView tableView, UIView headerView, nint section)
        {
            if(headerView != null)
            {
                headerView.TintColor = ThemeHelpers.ListHeaderBackgroundColor;
                headerView.BackgroundColor = ThemeHelpers.ListHeaderBackgroundColor;
                if(headerView is UITableViewHeaderFooterView hv)
                {
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
        }

        public override void WillDisplayFooterView(UITableView tableView, UIView footerView, nint section)
        {
            if(footerView != null)
            {
                footerView.TintColor = ThemeHelpers.ListHeaderBackgroundColor;
                footerView.BackgroundColor = ThemeHelpers.ListHeaderBackgroundColor;
                if(footerView is UITableViewHeaderFooterView fv)
                {
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
}
