using Bit.Core.Services;

namespace Bit.App.Controls
{
    public partial class AuthenticatorViewCell : BaseCipherViewCell
    {
        public AuthenticatorViewCell()
        {
            InitializeComponent();
        }

        protected override Image Icon => _iconImage;

        protected override IconLabel IconPlaceholder => _iconPlaceholderImage;

        private async void Image_OnLoaded(object sender, EventArgs e)
        {
            if (Handler?.MauiContext == null) { return; }
            if (_iconImage?.Source == null) { return; }

            try
            {
                var result = await _iconImage.Source.GetPlatformImageAsync(Handler.MauiContext);
                if (result == null)
                {
                    Icon_Error(sender, e);
                }
                else
                {
                    Icon_Success(sender, e);
                }
            }
            catch (InvalidOperationException) //Can occur with incorrect/malformed uris
            {
                Icon_Error(sender, e);
            }
            catch(Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                Icon_Error(sender, e);
            }
        }
    }
}
