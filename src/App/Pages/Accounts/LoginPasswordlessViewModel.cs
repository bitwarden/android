using System;
using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;
using Bit.App.Utilities;
using System.Linq;

namespace Bit.App.Pages
{
    public class LoginPasswordlessViewModel : BaseViewModel
    {
        private IStateService _stateService;

        public string Email { get; set; }
        public string LogInAttempByLabel { get; set; }
        public string FingerprintPhrase { get; set; }
        public FormattedString FingerprintPhraseFormatted { get; set; }
        public string DeviceType { get; set; }
        public string IpAddress { get; set; }
        public string NearLocation { get; set; }
        public string TimeOfRequest { get; set; }
        public DateTime RequestDate { get; set; }


        public LoginPasswordlessViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            PageTitle = AppResources.LogInRequested;
        }

        public async Task InitAsync()
        {
            LogInAttempByLabel = string.Format(AppResources.LogInAttemptByOn, Email, "bitwarden login test");
            FingerprintPhraseFormatted = CreateFingerprintPhrase();
            TimeOfRequest = CreateRequestDate();
            UpdateScreen();
        }

        private FormattedString CreateFingerprintPhrase()
        {
            var fingerprintList = FingerprintPhrase.Split('-').ToList();
            var fs = new FormattedString();
            var lastFingerprint = fingerprintList.LastOrDefault();

            foreach (var fingerprint in fingerprintList)
            {
                fs.Spans.Add(new Span
                {
                    Text = fingerprint
                });

                if(fingerprint == lastFingerprint)
                {
                    break;
                }

                fs.Spans.Add(new Span
                {
                    Text = "-",
                    TextColor = ThemeManager.GetResourceColor("DangerColor")
                });
            }
           
            return fs;
        }

        private string CreateRequestDate()
        {
            var minutesSinceRequest = RequestDate.ToUniversalTime().Minute - DateTime.UtcNow.Minute;
            if(minutesSinceRequest < 5)
            {
                return  AppResources.JustNow;
            }
            if(minutesSinceRequest < 59)
            {
                return $"{minutesSinceRequest} {AppResources.MinutesAgo}";
            }

            return RequestDate.ToShortTimeString();
        }

        public void UpdateScreen()
        {
            TriggerPropertyChanged(nameof(LogInAttempByLabel));
            TriggerPropertyChanged(nameof(FingerprintPhraseFormatted));
            TriggerPropertyChanged(nameof(DeviceType));
            TriggerPropertyChanged(nameof(IpAddress));
            TriggerPropertyChanged(nameof(NearLocation));
            TriggerPropertyChanged(nameof(TimeOfRequest));
        }
    }
}
