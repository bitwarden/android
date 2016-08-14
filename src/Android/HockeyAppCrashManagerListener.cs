using HockeyApp.Android;
using Bit.App.Abstractions;
using Newtonsoft.Json;

namespace Bit.Android
{
    public class HockeyAppCrashManagerListener : CrashManagerListener
    {
        private readonly IAppIdService _appIdService;
        private readonly IAuthService _authService;

        public HockeyAppCrashManagerListener(
            IAppIdService appIdService,
            IAuthService authService)
        {
            _appIdService = appIdService;
            _authService = authService;
        }

        public override string Description
        {
            get
            {
                var log = new
                {
                    AppId = _appIdService.AppId,
                    UserId = _authService.UserId
                };

                return JsonConvert.SerializeObject(log, Formatting.Indented);
            }
        }

        public override bool ShouldAutoUploadCrashes()
        {
            return true;
        }
    }
}