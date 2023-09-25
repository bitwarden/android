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
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var cipher = value as CipherView;
            return IconImageHelper.GetIconImage(cipher);
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }

    public static class IconImageHelper
    {
        public static string GetIconImage(CipherView cipher)
        {
            if (cipher.Type != CipherType.Login)
            {
                return null;
            }

            return GetLoginIconImage(cipher);
        }

        public static string GetLoginIconImage(CipherView cipher)
        {
            string image = null;
            if (cipher.Login.HasUris)
            {
                foreach (var uri in cipher.Login.Uris)
                {
                    var hostnameUri = uri.Uri;
                    var isWebsite = false;
                    if (!hostnameUri.Contains("."))
                    {
                        continue;
                    }
                    if (!hostnameUri.Contains("://"))
                    {
                        hostnameUri = string.Concat("http://", hostnameUri);
                    }
                    isWebsite = hostnameUri.StartsWith("http");

                    if (isWebsite)
                    {
                        image = GetIconUrl(hostnameUri);
                        break;
                    }
                }
            }
            return image;
        }

        private static string GetIconUrl(string hostnameUri)
        {
            IEnvironmentService _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");

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
            return string.Format("{0}/{1}/icon.png", iconsUrl, hostname);
        }
    }
}
