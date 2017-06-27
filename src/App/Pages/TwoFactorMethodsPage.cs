using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Models;

namespace Bit.App.Pages
{
    public class TwoFactorMethodsPage : ExtendedContentPage
    {
        private readonly string _email;
        private readonly FullLoginResult _result;

        public TwoFactorMethodsPage(string email, FullLoginResult result)
            : base(updateActivity: false)
        {
            _email = email;
            _result = result;

            Init();
        }

        public ExtendedTextCell AuthenticatorCell { get; set; }
        public ExtendedTextCell EmailCell { get; set; }
        public ExtendedTextCell DuoCell { get; set; }
        public ExtendedTextCell RecoveryCell { get; set; }

        private void Init()
        {
            var section = new TableSection(" ");

            if(_result.TwoFactorProviders.ContainsKey(Enums.TwoFactorProviderType.Authenticator))
            {
                AuthenticatorCell = new ExtendedTextCell
                {
                    Text = "Authenticator App",
                    Detail = "Use an authenticator app (such as Authy or Google Authenticator) to generate time-based verification codes."
                };
                section.Add(AuthenticatorCell);
            }

            if(_result.TwoFactorProviders.ContainsKey(Enums.TwoFactorProviderType.Duo))
            {
                DuoCell = new ExtendedTextCell
                {
                    Text = "Duo",
                    Detail = "Use duo."
                };
                section.Add(DuoCell);
            }

            if(_result.TwoFactorProviders.ContainsKey(Enums.TwoFactorProviderType.Email))
            {
                EmailCell = new ExtendedTextCell
                {
                    Text = "Email",
                    Detail = "Verification codes will be emailed to you."
                };
                section.Add(EmailCell);
            }

            RecoveryCell = new ExtendedTextCell
            {
                Text = "Recovery Code",
                Detail = "Lost access to all of your two-factor providers? Use your recovery code to disable all two-factor providers from your account."
            };
            section.Add(RecoveryCell);

            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    section
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 100;
            }

            Title = "Two-step Login Options";
            Content = table;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            if(AuthenticatorCell != null)
            {
                AuthenticatorCell.Tapped += AuthenticatorCell_Tapped;
            }
            if(DuoCell != null)
            {
                DuoCell.Tapped += DuoCell_Tapped;
            }
            if(EmailCell != null)
            {
                EmailCell.Tapped += EmailCell_Tapped;
            }
            RecoveryCell.Tapped += RecoveryCell_Tapped;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            if(AuthenticatorCell != null)
            {
                AuthenticatorCell.Tapped -= AuthenticatorCell_Tapped;
            }
            if(DuoCell != null)
            {
                DuoCell.Tapped -= DuoCell_Tapped;
            }
            if(EmailCell != null)
            {
                EmailCell.Tapped -= EmailCell_Tapped;
            }
            RecoveryCell.Tapped -= RecoveryCell_Tapped;
        }

        private void AuthenticatorCell_Tapped(object sender, EventArgs e)
        {
        }

        private void RecoveryCell_Tapped(object sender, EventArgs e)
        {
        }

        private void EmailCell_Tapped(object sender, EventArgs e)
        {
        }

        private void DuoCell_Tapped(object sender, EventArgs e)
        {
        }
    }
}
