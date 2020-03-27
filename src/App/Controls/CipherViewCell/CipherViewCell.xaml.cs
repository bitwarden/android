using Bit.App.Pages;
using Bit.Core;
using Bit.Core.Abstractions;
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

        public static readonly BindableProperty WebsiteIconsEnabledProperty = BindableProperty.Create(
            nameof(WebsiteIconsEnabled), typeof(bool), typeof(CipherViewCell), true, BindingMode.OneWay);

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(Command<CipherView>), typeof(CipherViewCell));

        private readonly IEnvironmentService _environmentService;

        private CipherViewCellViewModel _viewModel;
        private bool _usingNativeCell;

        public CipherViewCell()
        {
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            if (Device.RuntimePlatform == Device.iOS)
            {
                InitializeComponent();
                _viewModel = _grid.BindingContext as CipherViewCellViewModel;
            }
            else
            {
                _usingNativeCell = true;
            }
        }

        public bool WebsiteIconsEnabled
        {
            get => (bool)GetValue(WebsiteIconsEnabledProperty);
            set => SetValue(WebsiteIconsEnabledProperty, value);
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
            if (_usingNativeCell)
            {
                return;
            }
            if (propertyName == CipherProperty.PropertyName)
            {
                _viewModel.Cipher = Cipher;
            }
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();
            if (_usingNativeCell)
            {
                return;
            }

            _image.Source = null;
            CipherView cipher = null;
            if (BindingContext is GroupingsPageListItem groupingsPageListItem)
            {
                cipher = groupingsPageListItem.Cipher;
            }
            else if (BindingContext is CipherView cv)
            {
                cipher = cv;
            }
            if (cipher != null)
            {
                var iconImage = GetIconImage(cipher);
                if (iconImage.Item2 != null)
                {
                    _image.IsVisible = true;
                    _icon.IsVisible = false;
                    _image.Source = iconImage.Item2;
                    _image.LoadingPlaceholder = "login.png";
                }
                else
                {
                    _image.IsVisible = false;
                    _icon.IsVisible = true;
                    _icon.Text = iconImage.Item1;
                }
            }
        }

        public Tuple<string, string> GetIconImage(CipherView cipher)
        {
            string icon = null;
            string image = null;
            switch (cipher.Type)
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
            return new Tuple<string, string>(icon, image);
        }

        private Tuple<string, string> GetLoginIconImage(CipherView cipher)
        {
            string icon = "";
            string image = null;
            if (cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                var isWebsite = false;

                if (hostnameUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    icon = "";
                }
                else if (hostnameUri.StartsWith(Constants.iOSAppProtocol))
                {
                    icon = "";
                }
                else if (WebsiteIconsEnabled && !hostnameUri.Contains("://") && hostnameUri.Contains("."))
                {
                    hostnameUri = string.Concat("http://", hostnameUri);
                    isWebsite = true;
                }
                else if (WebsiteIconsEnabled)
                {
                    isWebsite = hostnameUri.StartsWith("http") && hostnameUri.Contains(".");
                }

                if (WebsiteIconsEnabled && isWebsite)
                {
                    var hostname = CoreHelpers.GetHostname(hostnameUri);
                    var iconsUrl = _environmentService.IconsUrl;
                    if (string.IsNullOrWhiteSpace(iconsUrl))
                    {
                        if (!string.IsNullOrWhiteSpace(_environmentService.BaseUrl))
                        {
                            iconsUrl = string.Format("{0}/icons", _environmentService.BaseUrl);
                        }
                        else
                        {
                            iconsUrl = "https://icons.bitwarden.net";
                        }
                    }
                    image = string.Format("{0}/{1}/icon.png", iconsUrl, hostname);
                }
            }
            return new Tuple<string, string>(icon, image);
        }

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            ButtonCommand?.Execute(Cipher);
        }
    }
}
