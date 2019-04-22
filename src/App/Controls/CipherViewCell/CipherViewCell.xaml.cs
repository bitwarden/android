using Bit.App.Pages;
using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class CipherViewCell : ViewCell
    {
        public static readonly BindableProperty CipherProperty = BindableProperty.Create(
            nameof(Cipher), typeof(CipherView), typeof(CipherViewCell), default(CipherView), BindingMode.OneWay);

        private CipherViewCellViewModel _viewModel;

        public CipherViewCell()
        {
            InitializeComponent();
            _viewModel = _grid.BindingContext as CipherViewCellViewModel;
        }

        public CipherView Cipher
        {
            get => GetValue(CipherProperty) as CipherView;
            set => SetValue(CipherProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if(propertyName == CipherProperty.PropertyName)
            {
                _viewModel.Cipher = Cipher;
            }
        }

        protected override void OnBindingContextChanged()
        {
            string icon = null;
            string image = null;

            _image.Source = null;
            if(BindingContext is GroupingsPageListItem groupingsPageListItem && groupingsPageListItem.Cipher != null)
            {
                switch(groupingsPageListItem.Cipher.Type)
                {
                    case CipherType.Login:
                        var loginIconImage = GetLoginIconImage(groupingsPageListItem.Cipher);
                        icon = loginIconImage.Item1;
                        image = loginIconImage.Item2;
                        break;
                    case CipherType.SecureNote:
                        icon = "&#xf24a;";
                        break;
                    case CipherType.Card:
                        icon = "&#xf09d;";
                        break;
                    case CipherType.Identity:
                        icon = "&#xf2c3;";
                        break;
                    default:
                        break;
                }

                if(image != null)
                {
                    _image.IsVisible = true;
                    _icon.IsVisible = false;
                    _image.Source = image;
                    _image.LoadingPlaceholder = "login.png";
                }
                else
                {
                    _image.IsVisible = false;
                    _icon.IsVisible = true;
                    _icon.Text = icon;
                }
            }
            base.OnBindingContextChanged();
        }

        private Tuple<string, string> GetLoginIconImage(CipherView cipher)
        {
            string icon = "&#xf0ac;";
            string image = null;
            var imageEnabled = true;
            if(cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                var isWebsite = false;

                if(hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = "&#xf17b;";
                }
                else if(hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = "&#xf179;";
                }
                else if(imageEnabled && !hostnameUri.Contains("://") && hostnameUri.Contains("."))
                {
                    hostnameUri = string.Concat("http://", hostnameUri);
                    isWebsite = true;
                }
                else if(imageEnabled)
                {
                    isWebsite = hostnameUri.StartsWith("http") && hostnameUri.Contains(".");
                }

                if(imageEnabled && isWebsite)
                {
                    var hostname = CoreHelpers.GetHostname(hostnameUri);
                    var iconsUrl = "https://icons.bitwarden.net";
                    image = string.Format("{0}/{1}/icon.png", iconsUrl, hostname);
                }
            }
            return new Tuple<string, string>(icon, image);
        }
    }
}
