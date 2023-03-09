using System;
using System.Linq;
using Bit.Core.Enums;

namespace Bit.Core.Utilities
{
    public struct OtpData
    {
        const string LABEL_SEPARATOR = ":";

        public OtpData(string absoluteUri)
        {
            if (!System.Uri.TryCreate(absoluteUri, UriKind.Absolute, out var uri)
                ||
                uri.Scheme != Constants.OtpAuthScheme)
            {
                throw new InvalidOperationException("Cannot create OtpData. Invalid OTP uri");
            }

            Uri = absoluteUri;
            AccountName = null;
            Issuer = null;
            Secret = null;
            Digits = null;
            Period = null;
            Algorithm = null;

            var escapedlabel = uri.Segments.Last();
            if (escapedlabel != "/")
            {
                var label = UriExtensions.UnescapeDataString(escapedlabel);
                if (label.Contains(LABEL_SEPARATOR))
                {
                    var parts = label.Split(LABEL_SEPARATOR);
                    Issuer = parts[0].Trim();
                    AccountName = parts[1].Trim();
                }
                else
                {
                    AccountName = label.Trim();
                }
            }

            var qsParams = CoreHelpers.GetQueryParams(uri);
            if (Issuer is null && qsParams.TryGetValue("issuer", out var issuer))
            {
                Issuer = issuer;
            }

            if (qsParams.TryGetValue("secret", out var secret))
            {
                Secret = secret;
            }

            if (qsParams.TryGetValue("digits", out var digitParam)
                &&
                int.TryParse(digitParam?.Trim(), out var digits))
            {
                Digits = digits;
            }

            if (qsParams.TryGetValue("period", out var periodParam)
                &&
                int.TryParse(periodParam?.Trim(), out var period)
                &&
                period > 0)
            {
                Period = period;
            }

            if (qsParams.TryGetValue("algorithm", out var algParam)
                &&
                algParam?.ToLower() is string alg)
            {
                if (alg == "sha256")
                {
                    Algorithm = CryptoHashAlgorithm.Sha256;
                }
                else if (alg == "sha512")
                {
                    Algorithm = CryptoHashAlgorithm.Sha512;
                }
            }
        }

        public string Uri { get; }
        public string AccountName { get; }
        public string Issuer { get; }
        public string Secret { get; }
        public int? Digits { get; }
        public int? Period { get; }
        public CryptoHashAlgorithm? Algorithm { get; }
    }
}
