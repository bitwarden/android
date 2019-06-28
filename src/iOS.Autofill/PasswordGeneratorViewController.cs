using System;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class PasswordGeneratorViewController : Core.Controllers.PasswordGeneratorViewController
    {
        public PasswordGeneratorViewController(IntPtr handle)
            : base(handle)
        { }

        public LoginAddViewController Parent { get; set; }
        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelBarButton;
        public override UIBarButtonItem BaseSelectBarButton => SelectBarButton;
        public override UILabel BasePasswordLabel => PasswordLabel;

        partial void SelectBarButton_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, () => Parent.PasswordCell.TextField.Text = PasswordLabel.Text);
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            DismissViewController(true, null);
        }
    }
}
