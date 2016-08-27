using HockeyApp.Android;
using Bit.App.Abstractions;
using Newtonsoft.Json;
using Android.Runtime;

namespace Bit.Android
{
    public class HockeyAppCrashManagerListener : CrashManagerListener
    {
        private readonly IAppIdService _appIdService;
        private readonly IAuthService _authService;

        public HockeyAppCrashManagerListener()
        { }

        public HockeyAppCrashManagerListener(System.IntPtr javaRef, JniHandleOwnership transfer)
        : base(javaRef, transfer)
        { }

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
                if(_appIdService != null && _authService != null)
                {
                    var log = new
                    {
                        AppId = _appIdService.AppId,
                        UserId = _authService.UserId
                    };

                    return JsonConvert.SerializeObject(log, Formatting.Indented);
                }
                else
                {
                    return null;
                }
            }
        }

        public override bool ShouldAutoUploadCrashes()
        {
            return true;
        }
    }
}