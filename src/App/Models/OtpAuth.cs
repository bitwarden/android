using Bit.App.Utilities;
using PCLCrypto;

namespace Bit.App.Models
{
    public class OtpAuth
    {
        public OtpAuth(string key)
        {
            if(key?.ToLowerInvariant().StartsWith("otpauth://") ?? false)
            {
                var qsParams = Helpers.GetQueryParams(key);
                if(qsParams.ContainsKey("digits") && qsParams["digits"] != null &&
                    int.TryParse(qsParams["digits"].Trim(), out var digitParam))
                {
                    if(digitParam > 10)
                    {
                        Digits = 10;
                    }
                    else if(digitParam > 0)
                    {
                        Digits = digitParam;
                    }
                }
                if(qsParams.ContainsKey("period") && qsParams["period"] != null &&
                    int.TryParse(qsParams["period"].Trim(), out var periodParam) && periodParam > 0)
                {
                    Period = periodParam;
                }
                if(qsParams.ContainsKey("secret") && qsParams["secret"] != null)
                {
                    Secret = qsParams["secret"];
                }
                if(qsParams.ContainsKey("algorithm") && qsParams["algorithm"] != null)
                {
                    var algParam = qsParams["algorithm"].ToLowerInvariant();
                    if(algParam == "sha256")
                    {
                        Algorithm = MacAlgorithm.HmacSha256;
                    }
                    else if(algParam == "sha512")
                    {
                        Algorithm = MacAlgorithm.HmacSha512;
                    }
                }
            }
            else
            {
                Secret = key;
            }
        }

        public int Period { get; set; } = 30;
        public int Digits { get; set; } = 6;
        public MacAlgorithm Algorithm { get; set; } = MacAlgorithm.HmacSha1;
        public string Secret { get; set; }
    }
}
