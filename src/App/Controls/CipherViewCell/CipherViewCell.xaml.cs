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

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(Command<CipherView>), typeof(CipherViewCell));

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

        public Command<CipherView> ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as Command<CipherView>;
            set => SetValue(ButtonCommandProperty, value);
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

            CipherView cipher = null;
            if(BindingContext is GroupingsPageListItem groupingsPageListItem)
            {
                cipher = groupingsPageListItem.Cipher;
            }
            else if(BindingContext is CipherView cv)
            {
                cipher = cv;
            }
            if(cipher != null)
            {
                switch(cipher.Type)
                {
                    case CipherType.Login:
                        var loginIconImage = GetLoginIconImage(cipher);
                        icon = loginIconImage.Item1;
                        image = loginIconImage.Item2;
                        break;
                    case CipherType.SecureNote:
                        icon = "";
                        break;
                    case CipherType.Card:
                        icon = "";
                        break;
                    case CipherType.Identity:
                        icon = "";
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
            string icon = "";
            string image = null;
            var imageEnabled = true;
            if(cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                var isWebsite = false;

                if(hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = "";
                }
                else if(hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = "";
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

        private void ImageButton_Clicked(object sender, EventArgs e)
        {
            ButtonCommand?.Execute(Cipher);
        }
    }
}
