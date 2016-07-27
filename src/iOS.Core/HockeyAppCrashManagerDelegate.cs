using Bit.App.Abstractions;
using HockeyApp.iOS;
using Newtonsoft.Json;

namespace Bit.iOS.Core
{
    public class HockeyAppCrashManagerDelegate : BITCrashManagerDelegate
    {
        private readonly IAppIdService _appIdService;
        private readonly IAuthService _authService;

        public HockeyAppCrashManagerDelegate(
            IAppIdService appIdService,
            IAuthService authService)
        {
            _appIdService = appIdService;
            _authService = authService;
        }

        public override string ApplicationLogForCrashManager(BITCrashManager crashManager)
        {
            var log = new
            {
                AppId = _appIdService.AppId,
                UserId = _authService.UserId
            };

            return JsonConvert.SerializeObject(log, Formatting.Indented);
        }
    }
}
