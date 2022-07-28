using System;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginPasswordlessViewModel : BaseViewModel
    {
        private IAuthService _authService;
        private IPlatformUtilsService _platformUtilsService;
        private ILogger _logger;
        private string _logInAttempByLabel;
        private string _deviceType;
        private FormattedString _fingerprintPhraseFormatted;
        private string _fingerprintPhrase;
        private string _email;
        private string _timeOfRequest;
        private DateTime _requestDate;
        private string _nearLocation;
        private string _ipAddress;

        public LoginPasswordlessViewModel()
        {
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");

            PageTitle = AppResources.LogInRequested;

            AcceptRequestCommand = new AsyncCommand(AcceptRequestAsync,
                onException: ex => _logger.Exception(ex),
                allowsMultipleExecutions: false);
            RejectRequestCommand = new AsyncCommand(RejectRequestAsync,
                onException: ex => _logger.Exception(ex),
                allowsMultipleExecutions: false);
        }

        public ICommand AcceptRequestCommand { get; }

        public ICommand RejectRequestCommand { get; }

        public string Email
        {
            get => _email;
            set
            {
                LogInAttempByLabel = string.Format(AppResources.LogInAttemptByOn, value, "bitwarden login test");
                SetProperty(ref _email, value);
            }
        }

        public string FingerprintPhrase
        {
            get => _fingerprintPhrase;
            set
            {
                FingerprintPhraseFormatted = CreateFingerprintPhrase(value);
                SetProperty(ref _fingerprintPhrase, value);
            }
        }

        public FormattedString FingerprintPhraseFormatted
        {
            get => _fingerprintPhraseFormatted;
            set => SetProperty(ref _fingerprintPhraseFormatted, value);
        }

        public string LogInAttempByLabel
        {
            get => _logInAttempByLabel;
            set => SetProperty(ref _logInAttempByLabel, value);
        }

        public string DeviceType
        {
            get => _deviceType;
            set => SetProperty(ref _deviceType, value);
        }

        public string IpAddress
        {
            get => _ipAddress;
            set => SetProperty(ref _ipAddress, value);
        }

        public string NearLocation
        {
            get => _nearLocation;
            set => SetProperty(ref _nearLocation, value);
        }

        public DateTime RequestDate
        {
            get => _requestDate;
            set
            {
                TimeOfRequestText = CreateRequestDate();
                SetProperty(ref _requestDate, value);
            }
        }

        public string TimeOfRequestText
        {
            get => _timeOfRequest;
            set
            {
                SetProperty(ref _timeOfRequest, value);
            }
        }

        private FormattedString CreateFingerprintPhrase(string fingerprintPhrase)
        {
            var fingerprintList = fingerprintPhrase.Split('-').ToList();
            var fs = new FormattedString();
            var lastFingerprint = fingerprintList.LastOrDefault();

            foreach (var fingerprint in fingerprintList)
            {
                fs.Spans.Add(new Span
                {
                    Text = fingerprint
                });

                if (fingerprint == lastFingerprint)
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
            if (minutesSinceRequest < 5)
            {
                return AppResources.JustNow;
            }
            if (minutesSinceRequest < 59)
            {
                return $"{minutesSinceRequest} {AppResources.MinutesAgo}";
            }

            return RequestDate.ToShortTimeString();
        }

        private async Task AcceptRequestAsync()
        {
            try
            {
                var res = await _authService.LogInPasswordlessAcceptAsync();
                await Page.Navigation.PopModalAsync();
                _platformUtilsService.ShowToast("info", null, AppResources.LogInAccepted);
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private async Task RejectRequestAsync()
        {
            try
            {
                var res = await _authService.LogInPasswordlessRejectAsync();
                await Page.Navigation.PopModalAsync();
                _platformUtilsService.ShowToast("info", null, AppResources.LogInDenied);
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }
    }
}
