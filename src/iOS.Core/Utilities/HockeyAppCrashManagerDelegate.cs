using Bit.Core.Abstractions;
using HockeyApp.iOS;
using Newtonsoft.Json;
using System.Threading.Tasks;

namespace Bit.iOS.Core.Utilities
{
    public class HockeyAppCrashManagerDelegate : BITCrashManagerDelegate
    {
        private const string HockeyAppId = "51f96ae568ba45f699a18ad9f63046c3";

        private readonly IAppIdService _appIdService;
        private readonly IUserService _userService;

        private string _userId;
        private string _appId;

        public HockeyAppCrashManagerDelegate(
            IAppIdService appIdService,
            IUserService userService)
        {
            _appIdService = appIdService;
            _userService = userService;
        }

        public async Task InitAsync()
        {
            _userId = await _userService.GetUserIdAsync();
            _appId = await _appIdService.GetAppIdAsync();
            var manager = BITHockeyManager.SharedHockeyManager;
            manager.Configure(HockeyAppId, this);
            manager.CrashManager.CrashManagerStatus = BITCrashManagerStatus.AutoSend;
            manager.UserId = _userId;
            manager.Authenticator.AuthenticateInstallation();
            manager.DisableMetricsManager = manager.DisableFeedbackManager = manager.DisableUpdateManager = true;
            manager.StartManager();
        }

        public override string ApplicationLogForCrashManager(BITCrashManager crashManager)
        {
            return JsonConvert.SerializeObject(new
            {
                AppId = _appId,
                UserId = _userId
            }, Formatting.Indented);
        }
    }
}
