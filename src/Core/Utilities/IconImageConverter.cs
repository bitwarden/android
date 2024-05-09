using System;
using System.Globalization;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Nager.PublicSuffix;

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
                foreach (var uri in cipher.Login.Uris.Where(u => u.Uri != null))
                {
                    var domain = GetValidDomainOrNull(uri.Uri);
                    if (domain != null)
                    {
                        image = GetIconUrl(domain);
                        break;
                    }
                }
            }

            return image;
        }

        // TODO: Assumes that only valid domains (not IP addresses) have a favicon.
        // This might be shortsighted in the event that
        //     a) a webservice is hosted on an IP
        //     b) the icon server is user-supplied and has access to intranet services etc.
        private static string GetValidDomainOrNull(string uriString)
        {
            var domainParser = ServiceContainer.Resolve<IDomainParser>();
            var uri = CoreHelpers.GetUri(uriString);
            if (uri == null)
                return null;

            if (uri.Host.EndsWith(".onion") || uri.Host.EndsWith(".i2p"))
                return null;

            var domainInfo = domainParser.Parse(uri.Host);
            return domainInfo?.RegistrableDomain;
        }

        // TODO: Getting the service and re-formatting the icon API string doesn't have to be done for every single requested domain, right?
        private static string GetIconUrl(string domain)
        {
            IEnvironmentService _environmentService =
                ServiceContainer.Resolve<IEnvironmentService>("environmentService");

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

            return string.Format("{0}/{1}/icon.png", iconsUrl, domain);
        }
    }
}
