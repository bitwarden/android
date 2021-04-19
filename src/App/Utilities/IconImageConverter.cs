using System;
using System.Globalization;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class IconImageConverter : IValueConverter
    {
        private readonly IEnvironmentService _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");

        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var cipher = value as CipherView;
            return GetIcon(cipher);
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }

        private string GetIcon(CipherView cipher)
        {
            string icon = null;
            switch (cipher.Type)
            {
                case CipherType.Login:
                    icon = GetLoginIconImage(cipher);
                    break;
                default:
                    break;
            }
            return icon;
        }

        string GetLoginIconImage(CipherView cipher)
        {
            string image = null;
            if (cipher.Login.Uri != null)
            {
                var hostnameUri = cipher.Login.Uri;
                var isWebsite = false;

                if (!hostnameUri.Contains("://") && hostnameUri.Contains("."))
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
            return image;
        }
    }
}
