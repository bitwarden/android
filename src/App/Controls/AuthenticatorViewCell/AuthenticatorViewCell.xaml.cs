using System;
using Bit.App.Pages;
using Bit.App.Utilities;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class AuthenticatorViewCell : ExtendedGrid
    {
        public static readonly BindableProperty CipherProperty = BindableProperty.Create(
            nameof(Cipher), typeof(CipherView), typeof(AuthenticatorViewCell), default(CipherView), BindingMode.TwoWay);

        public static readonly BindableProperty WebsiteIconsEnabledProperty = BindableProperty.Create(
            nameof(WebsiteIconsEnabled), typeof(bool?), typeof(AuthenticatorViewCell));

        public static readonly BindableProperty TotpSecProperty = BindableProperty.Create(
            nameof(TotpSec), typeof(long), typeof(AuthenticatorViewCell));

        public AuthenticatorViewCell()
        {
            InitializeComponent();
        }

        public Command CopyCommand { get; set; }

        public CipherView Cipher
        {
            get => GetValue(CipherProperty) as CipherView;
            set => SetValue(CipherProperty, value);
        }

        public bool? WebsiteIconsEnabled
        {
            get => (bool)GetValue(WebsiteIconsEnabledProperty);
            set => SetValue(WebsiteIconsEnabledProperty, value);
        }

        public long TotpSec
        {
            get => (long)GetValue(TotpSecProperty);
            set => SetValue(TotpSecProperty, value);
        }

        public bool ShowIconImage
        {
            get => WebsiteIconsEnabled ?? false
                && !string.IsNullOrWhiteSpace(Cipher.Login?.Uri)
                && IconImageSource != null;
        }

        private string _iconImageSource = string.Empty;
        public string IconImageSource
        {
            get
            {
                if (_iconImageSource == string.Empty) // default value since icon source can return null
                {
                    _iconImageSource = IconImageHelper.GetLoginIconImage(Cipher);
                }
                return _iconImageSource;
            }

        }
    }
}
