using Bit.Core;
using Bit.Core.Services;
using Bit.iOS.Core.Controllers;
using Bit.iOS.Core.Utilities;
using ObjCRuntime;
using UIKit;

namespace Bit.iOS.Core.Views
{
    public static class NSLayoutConstraintExt
    {
        public static NSLayoutConstraint WithId(this NSLayoutConstraint constraint, string id)
        {
            constraint.SetIdentifier(id);
            return constraint;
        }
    }

    public class CipherLoginTableViewCell : ExtendedUITableViewCell
    {
        private readonly UILabel _title = new UILabel();
        private readonly UILabel _subtitle = new UILabel();
        private readonly UILabel _mainIcon = new UILabel();
        private readonly UILabel _orgIcon = new UILabel();
        private readonly UIView _separator = new UIView();

        private UIStackView _mainStackView, _titleStackView;

        protected internal CipherLoginTableViewCell(NativeHandle handle) : base(handle)
        {
            Setup();
        }

        private void Setup()
        {
            try
            {
                _title.TextColor = ThemeHelpers.TextColor;
                _title.Font = UIFont.SystemFontOfSize(14);
                _title.LineBreakMode = UILineBreakMode.TailTruncation;
                _title.Lines = 1;

                _subtitle.TextColor = ThemeHelpers.MutedColor;
                _subtitle.Font = UIFont.SystemFontOfSize(12);
                _subtitle.LineBreakMode = UILineBreakMode.TailTruncation;
                _subtitle.Lines = 1;

                _mainIcon.Font = UIFont.FromName("bwi-font", 24);
                _mainIcon.AdjustsFontSizeToFitWidth = true;
                _mainIcon.Text = BitwardenIcons.Globe;
                _mainIcon.TextColor = ThemeHelpers.MutedColor;

                _orgIcon.Font = UIFont.FromName("bwi-font", 15);
                _orgIcon.TextColor = ThemeHelpers.MutedColor;
                _orgIcon.Text = BitwardenIcons.Collection;
                _orgIcon.Hidden = true;

                _separator.BackgroundColor = ThemeHelpers.SeparatorColor;

                _titleStackView = new UIStackView(new UIView[] { _title, _orgIcon })
                {
                    Axis = UILayoutConstraintAxis.Horizontal,
                    Spacing = 4
                };

                _mainStackView = new UIStackView(new UIView[] { _titleStackView, _subtitle })
                {
                    Axis = UILayoutConstraintAxis.Vertical
                };

                _mainIcon.TranslatesAutoresizingMaskIntoConstraints = false;
                _separator.TranslatesAutoresizingMaskIntoConstraints = false;
                _mainStackView.TranslatesAutoresizingMaskIntoConstraints = false;

                ContentView.AddSubview(_mainStackView);
                ContentView.AddSubview(_mainIcon);
                ContentView.AddSubview(_separator);

                NSLayoutConstraint.ActivateConstraints(new NSLayoutConstraint[]
                {
                    _mainIcon.LeadingAnchor.ConstraintEqualTo(ContentView.LeadingAnchor, 9),
                    _mainIcon.CenterYAnchor.ConstraintEqualTo(ContentView.CenterYAnchor),
                    _mainIcon.WidthAnchor.ConstraintEqualTo(31),
                    _mainIcon.HeightAnchor.ConstraintEqualTo(31),

                    _mainStackView.LeadingAnchor.ConstraintEqualTo(_mainIcon.TrailingAnchor, 3),
                    _mainStackView.TopAnchor.ConstraintEqualTo(ContentView.TopAnchor, 8),
                    _mainStackView.TrailingAnchor.ConstraintLessThanOrEqualTo(ContentView.TrailingAnchor, 9),

                    _separator.HeightAnchor.ConstraintEqualTo(1),
                    _separator.TopAnchor.ConstraintEqualTo(_mainStackView.BottomAnchor, 8),
                    _separator.LeadingAnchor.ConstraintEqualTo(ContentView.LeadingAnchor, 7),
                    _separator.TrailingAnchor.ConstraintEqualTo(ContentView.TrailingAnchor, -7),
                    _separator.BottomAnchor.ConstraintEqualTo(ContentView.BottomAnchor, 0)
                });
            }
            catch (System.Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public void SetTitle(string title) => _title.Text = title;

        public void SetSubtitle(string subtitle) => _subtitle.Text = subtitle;

        public void UpdateMainIcon(bool usePasskeyIcon) => _mainIcon.Text = usePasskeyIcon ? BitwardenIcons.Passkey : BitwardenIcons.Globe;
        
        public void ShowOrganizationIcon() => _orgIcon.Hidden = false;
    }
}
