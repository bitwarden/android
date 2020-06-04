using System;
using System.Globalization;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class IconConverter : IValueConverter
    {
        private readonly IEnvironmentService _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");

        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {

            var cipher = value as CipherView;
            var iconImage = GetIconImage(cipher);
            if (iconImage.Item2 != null)
            {

                return iconImage.Item2;
            }
            else
            {
                return iconImage.Item1;
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
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

        Tuple<string, string> GetLoginIconImage(CipherView cipher)
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
                else if (!hostnameUri.Contains("://") && hostnameUri.Contains("."))
                {
                    hostnameUri = string.Concat("http://", hostnameUri);
                    isWebsite = true;
                }
                else
                {
                    isWebsite = hostnameUri.StartsWith("http") && hostnameUri.Contains(".");
                }

                if (isWebsite)
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
    }
}