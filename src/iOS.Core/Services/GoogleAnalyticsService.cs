using System;
using Bit.App.Abstractions;
using Google.Analytics;

namespace Bit.iOS.Core.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        private readonly ITracker _tracker;
        private readonly IAuthService _authService;
        private bool _setUserId = true;

        public GoogleAnalyticsService(
            IAppIdService appIdService,
            IAuthService authService)
        {
            _authService = authService;

            Gai.SharedInstance.DispatchInterval = 10;
            Gai.SharedInstance.TrackUncaughtExceptions = true;
            _tracker = Gai.SharedInstance.GetTracker("UA-81915606-1");
            _tracker.Set(GaiConstants.ClientId, appIdService.AppId);
        }

        public void RefreshUserId()
        {
            _tracker.Set(GaiConstants.UserId, null);
            _setUserId = true;
        }

        public void TrackEvent(string category, string eventName)
        {
            SetUserId();
            var dict = DictionaryBuilder.CreateEvent(category, eventName, "AppEvent", null).Build();
            _tracker.Send(dict);
            Gai.SharedInstance.Dispatch();
        }

        public void TrackException(string message, bool fatal)
        {
            SetUserId();
            var dict = DictionaryBuilder.CreateException(message, fatal).Build();
            _tracker.Send(dict);
        }

        public void TrackPage(string pageName)
        {
            SetUserId();
            _tracker.Set(GaiConstants.ScreenName, pageName);
            var dict = DictionaryBuilder.CreateScreenView().Build();
            _tracker.Send(dict);
        }

        private void SetUserId()
        {
            if(_setUserId && _authService.IsAuthenticated)
            {
                _tracker.Set(GaiConstants.UserId, _authService.UserId);
                _setUserId = false;
            }
        }
    }
}
