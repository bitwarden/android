namespace Bit.App.Controls
{
    public partial class AuthenticatorViewCell : BaseCipherViewCell
    {
        public AuthenticatorViewCell()
        {
            InitializeComponent();
        }

        protected override CachedImage Icon => _iconImage;

        protected override IconLabel IconPlaceholder => _iconPlaceholderImage;
    }
}
