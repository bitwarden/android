using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Utilities
{
    public class TotpHelper
    {
        private ITotpService _totpService;
        private int _interval;

        public TotpHelper(CipherView cipher)
        {
            _totpService = ServiceContainer.Resolve<ITotpService>("totpService");
            Cipher = cipher;
            _interval = _totpService.GetTimeInterval(cipher?.Login?.Totp);
        }

        public CipherView Cipher { get; private set; }
        public string TotpSec { get; private set; }
        public string TotpCodeFormatted { get; private set; }
        public double Progress { get; private set; }

        public async Task GenerateNewTotpValues()
        {
            var epoc = CoreHelpers.EpocUtcNow() / 1000;
            var mod = epoc % _interval;
            var totpSec = _interval - mod;
            TotpSec = totpSec.ToString();
            Progress = totpSec * 100 / 30;
            if (mod == 0 || string.IsNullOrEmpty(TotpCodeFormatted))
            {
                TotpCodeFormatted = await TotpUpdateCodeAsync();
            }
        }

        private async Task<string> TotpUpdateCodeAsync()
        {
            var totpCode = await _totpService.GetCodeAsync(Cipher?.Login?.Totp);
            if (totpCode != null)
            {
                if (totpCode.Length > 4)
                {
                    var half = (int)Math.Floor(totpCode.Length / 2M);
                    return string.Format("{0} {1}", totpCode.Substring(0, half),
                        totpCode.Substring(half));
                }
                else
                {
                    return totpCode;
                }
            }
            else
            {
                return null;
            }
        }
    }
}
