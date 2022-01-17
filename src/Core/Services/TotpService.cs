using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class TotpService : ITotpService
    {
        private const string SteamChars = "23456789BCDFGHJKMNPQRTVWXY";

        private readonly IStateService _stateService;
        private readonly ICryptoFunctionService _cryptoFunctionService;

        public TotpService(
            IStateService stateService,
            ICryptoFunctionService cryptoFunctionService)
        {
            _stateService = stateService;
            _cryptoFunctionService = cryptoFunctionService;
        }

        public async Task<string> GetCodeAsync(string key)
        {
            if (string.IsNullOrWhiteSpace(key))
            {
                return null;
            }
            var period = 30;
            var alg = CryptoHashAlgorithm.Sha1;
            var digits = 6;
            var keyB32 = key;

            var isOtpAuth = key?.ToLowerInvariant().StartsWith("otpauth://") ?? false;
            var isSteamAuth = key?.ToLowerInvariant().StartsWith("steam://") ?? false;
            if (isOtpAuth)
            {
                var qsParams = CoreHelpers.GetQueryParams(key);
                if (qsParams.ContainsKey("digits") && qsParams["digits"] != null &&
                    int.TryParse(qsParams["digits"].Trim(), out var digitParam))
                {
                    if (digitParam > 10)
                    {
                        digits = 10;
                    }
                    else if (digitParam > 0)
                    {
                        digits = digitParam;
                    }
                }
                if (qsParams.ContainsKey("period") && qsParams["period"] != null &&
                    int.TryParse(qsParams["period"].Trim(), out var periodParam) && periodParam > 0)
                {
                    period = periodParam;
                }
                if (qsParams.ContainsKey("secret") && qsParams["secret"] != null)
                {
                    keyB32 = qsParams["secret"];
                }
                if (qsParams.ContainsKey("algorithm") && qsParams["algorithm"] != null)
                {
                    var algParam = qsParams["algorithm"].ToLowerInvariant();
                    if (algParam == "sha256")
                    {
                        alg = CryptoHashAlgorithm.Sha256;
                    }
                    else if (algParam == "sha512")
                    {
                        alg = CryptoHashAlgorithm.Sha512;
                    }
                }
            }
            else if (isSteamAuth)
            {
                digits = 5;
                keyB32 = key.Substring(8);
            }

            var keyBytes = Base32.FromBase32(keyB32);
            if (keyBytes == null || keyBytes.Length == 0)
            {
                return null;
            }
            var now = CoreHelpers.EpocUtcNow() / 1000;
            var time = now / period;
            var timeBytes = BitConverter.GetBytes(time);
            if (BitConverter.IsLittleEndian)
            {
                Array.Reverse(timeBytes, 0, timeBytes.Length);
            }

            var hash = await _cryptoFunctionService.HmacAsync(timeBytes, keyBytes, alg);
            if (hash.Length == 0)
            {
                return null;
            }

            var offset = (hash[hash.Length - 1] & 0xf);
            var binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

            string otp = string.Empty;
            if (isSteamAuth)
            {
                var fullCode = binary & 0x7fffffff;
                for (var i = 0; i < digits; i++)
                {
                    otp += SteamChars[fullCode % SteamChars.Length];
                    fullCode = (int)Math.Truncate(fullCode / (double)SteamChars.Length);
                }
            }
            else
            {
                var rawOtp = binary % (int)Math.Pow(10, digits);
                otp = rawOtp.ToString().PadLeft(digits, '0');
            }
            return otp;
        }

        public int GetTimeInterval(string key)
        {
            var period = 30;
            if (key != null && key.ToLowerInvariant().StartsWith("otpauth://"))
            {
                var qsParams = CoreHelpers.GetQueryParams(key);
                if (qsParams.ContainsKey("period") && qsParams["period"] != null &&
                    int.TryParse(qsParams["period"].Trim(), out var periodParam) && periodParam > 0)
                {
                    period = periodParam;
                }
            }
            return period;
        }

        public async Task<bool> IsAutoCopyEnabledAsync()
        {
            var disabled = await _stateService.GetDisableAutoTotpCopyAsync();
            return !disabled.GetValueOrDefault();
        }
    }
}
