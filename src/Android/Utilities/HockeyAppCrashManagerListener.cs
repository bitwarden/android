#if !FDROID
using HockeyApp.Android;
using Bit.App.Abstractions;
using Newtonsoft.Json;
using Android.Runtime;
using Bit.Core.Abstractions;
using System.Threading.Tasks;
using Android.Content;

namespace Bit.Droid.Utilities
{
    public class HockeyAppCrashManagerListener : CrashManagerListener
    {
        private const string HockeyAppId = "d3834185b4a643479047b86c65293d42";

        private readonly IAppIdService _appIdService;
        private readonly IUserService _userService;

        private string _userId;
        private string _appId;

        public HockeyAppCrashManagerListener()
        { }

        public HockeyAppCrashManagerListener(System.IntPtr javaRef, JniHandleOwnership transfer)
            : base(javaRef, transfer)
        { }

        public HockeyAppCrashManagerListener(
            IAppIdService appIdService,
            IUserService userService)
        {
            _appIdService = appIdService;
            _userService = userService;
        }

        public async Task InitAsync(Context context)
        {
            _userId = await _userService.GetUserIdAsync();
            _appId = await _appIdService.GetAppIdAsync();
            CrashManager.Register(context, HockeyAppId, this);
        }

        public override string Description
        {
            get
            {
                if(_userId != null && _appId != null)
                {
                    return JsonConvert.SerializeObject(new
                    {
                        AppId = _appId,
                        UserId = _userId
                    }, Formatting.Indented);
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
#endif
