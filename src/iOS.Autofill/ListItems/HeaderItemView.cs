using Bit.Core.Services;
using Foundation;
using ObjCRuntime;
using UIKit;

namespace Bit.iOS.Autofill.ListItems
{
    public class HeaderItemView : UITableViewHeaderFooterView
    {
        private readonly UILabel _header = new UILabel();
        private readonly UIView _separator = new UIView();

        public HeaderItemView(NSString reuseIdentifier)
            : base(reuseIdentifier)
        {
            Setup();
        }

        protected internal HeaderItemView(NativeHandle handle) : base(handle)
        {
            Setup();
        }

        public void SetHeaderText(string text) => _header.Text = text;

        private void Setup()
        {
            try
            {
                _header.TextColor = UIColor.FromName(ColorConstants.LIGHT_TEXT_MUTED);
                _header.Font = UIFont.SystemFontOfSize(15);
                _separator.BackgroundColor = UIColor.FromName(ColorConstants.LIGHT_SECONDARY_300);

                _header.TranslatesAutoresizingMaskIntoConstraints = false;
                _separator.TranslatesAutoresizingMaskIntoConstraints = false;

                ContentView.AddSubview(_header);
                ContentView.AddSubview(_separator);

                NSLayoutConstraint.ActivateConstraints(new NSLayoutConstraint[]
                {
                    _header.LeadingAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.LeadingAnchor, 9),
                    _header.TrailingAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.TrailingAnchor, 9),
                    _header.TopAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.TopAnchor, 3),

                    _separator.HeightAnchor.ConstraintEqualTo(2),
                    _separator.TopAnchor.ConstraintEqualTo(_header.BottomAnchor, 8),
                    _separator.LeadingAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.LeadingAnchor, 5),
                    _separator.TrailingAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.TrailingAnchor, 5),
                    _separator.BottomAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.BottomAnchor, 2)
                });
            }
            catch (System.Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }
    }
}
