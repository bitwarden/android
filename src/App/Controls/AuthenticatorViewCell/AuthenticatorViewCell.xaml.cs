using System;
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

        //public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
        //    nameof(ButtonCommand), typeof(Command<CipherView>), typeof(AuthenticatorViewCell));

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

        private string _totpCodeFormatted = "938 928";
        public string TotpCodeFormatted
        {
            get => _totpCodeFormatted;
            set => _totpCodeFormatted = value;
        }


        //public Command<CipherView> ButtonCommand
        //{
        //    get => GetValue(ButtonCommandProperty) as Command<CipherView>;
        //    set => SetValue(ButtonCommandProperty, value);
        //}

        //protected override void OnPropertyChanged(string propertyName = null)
        //{
        //    base.OnPropertyChanged(propertyName);
        //    if (propertyName == CipherProperty.PropertyName)
        //    {
        //        if (Cipher == null)
        //        {
        //            return;
        //        }
        //        _cipherLabel.Text = Cipher.Name;
        //    }
        //    else if (propertyName == WebsiteIconsEnabledProperty.PropertyName)
        //    {
        //        if (Cipher == null)
        //        {
        //            return;
        //        }
        //        ((AuthenticatorViewCellViewModel)BindingContext).WebsiteIconsEnabled = WebsiteIconsEnabled ?? false;
        //    }
        //    else if (propertyName == TotpSecProperty.PropertyName)
        //    {
        //        if (Cipher == null)
        //        {
        //            return;
        //        }
        //        ((AuthenticatorViewCellViewModel)BindingContext).UpdateTotpSec(TotpSec);
        //    }
        //}

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            var cipher = ((sender as MiButton)?.BindingContext as AuthenticatorViewCellViewModel)?.Cipher;
            if (cipher != null)
            {
                //ButtonCommand?.Execute(cipher);
            }
        }
    }
}
