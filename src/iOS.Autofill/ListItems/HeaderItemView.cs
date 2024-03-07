using Bit.Core.Services;
using Bit.iOS.Core.Utilities;
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
                _header.TextColor = ThemeHelpers.TextColor;
                _header.Font = UIFont.SystemFontOfSize(15, UIFontWeight.Semibold);
                _separator.BackgroundColor = ThemeHelpers.SeparatorColor;

                _header.TranslatesAutoresizingMaskIntoConstraints = false;
                _separator.TranslatesAutoresizingMaskIntoConstraints = false;

                ContentView.AddSubview(_header);
                ContentView.AddSubview(_separator);

                NSLayoutConstraint.ActivateConstraints(new NSLayoutConstraint[]
                {
                    _header.LeadingAnchor.ConstraintEqualTo(ContentView.LeadingAnchor, 9),
                    _header.TrailingAnchor.ConstraintEqualTo(ContentView.TrailingAnchor, 9),
                    _header.TopAnchor.ConstraintEqualTo(ContentView.LayoutMarginsGuide.TopAnchor, 10),

                    _separator.HeightAnchor.ConstraintEqualTo(1),
                    _separator.TopAnchor.ConstraintEqualTo(_header.BottomAnchor, 12),
                    _separator.LeadingAnchor.ConstraintEqualTo(ContentView.LeadingAnchor, 7),
                    _separator.TrailingAnchor.ConstraintEqualTo(ContentView.TrailingAnchor, -7),
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
