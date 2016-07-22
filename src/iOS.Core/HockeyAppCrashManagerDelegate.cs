using Bit.App.Abstractions;
using HockeyApp.iOS;
namespace Bit.iOS.Core
{
    public class HockeyAppCrashManagerDelegate : BITCrashManagerDelegate
    {
        private readonly IAppIdService _appIdService;

        public HockeyAppCrashManagerDelegate(
            IAppIdService appIdService)
        {
            _appIdService = appIdService;
        }

        public override string ApplicationLogForCrashManager(BITCrashManager crashManager)
        {
            return $"AppId:{_appIdService.AppId}";
        }
    }
}
