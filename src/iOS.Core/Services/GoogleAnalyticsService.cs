using System;
using Bit.App.Abstractions;
using Google.Analytics;

namespace Bit.iOS.Core.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        private readonly ITracker _tracker;
        private readonly IAuthService _authService;

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

        public bool SetUserId { get; set; } = true;

        public void TrackEvent(string category, string eventName)
        {
            if(SetUserId)
            {
                _tracker.Set(GaiConstants.UserId, _authService.UserId);
                SetUserId = false;
            }
            var dict = DictionaryBuilder.CreateEvent(category, eventName, "AppEvent", null).Build();
            _tracker.Send(dict);
            Gai.SharedInstance.Dispatch();
        }

        public void TrackException(string message, bool fatal)
        {
            if(SetUserId)
            {
                _tracker.Set(GaiConstants.UserId, _authService.UserId);
                SetUserId = false;
            }
            var dict = DictionaryBuilder.CreateException(message, fatal).Build();
            _tracker.Send(dict);
        }

        public void TrackPage(string pageName)
        {
            if(SetUserId)
            {
                _tracker.Set(GaiConstants.UserId, _authService.UserId);
                SetUserId = false;
            }
            _tracker.Set(GaiConstants.ScreenName, pageName);
            var dict = DictionaryBuilder.CreateScreenView().Build();
            _tracker.Send(dict);
        }
    }
}
