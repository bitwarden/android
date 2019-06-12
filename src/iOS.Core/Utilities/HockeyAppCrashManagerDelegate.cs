using Bit.Core.Abstractions;
using HockeyApp.iOS;
using Newtonsoft.Json;
using System.Threading.Tasks;

namespace Bit.iOS.Core.Utilities
{
    public class HockeyAppCrashManagerDelegate : BITCrashManagerDelegate
    {
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

        public async Task InitAsync(BITHockeyManager manager)
        {
            _userId = await _userService.GetUserIdAsync();
            _appId = await _appIdService.GetAppIdAsync();
            manager.UserId = _userId;
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
