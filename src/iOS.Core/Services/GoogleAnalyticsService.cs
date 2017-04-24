using System;
using Bit.App.Abstractions;
using Google.Analytics;
using Plugin.Settings.Abstractions;

namespace Bit.iOS.Core.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        private readonly ITracker _tracker;
        private readonly IAuthService _authService;

        public GoogleAnalyticsService(
            IAppIdService appIdService,
            IAuthService authService,
            ISettings settings)
        {
            _authService = authService;

            Gai.SharedInstance.DispatchInterval = 10;
            Gai.SharedInstance.TrackUncaughtExceptions = false;
            _tracker = Gai.SharedInstance.GetTracker("UA-81915606-1");
            _tracker.SetAllowIdfaCollection(true);
            _tracker.Set(GaiConstants.ClientId, appIdService.AnonymousAppId);

            var gaOptOut = settings.GetValueOrDefault(App.Constants.SettingGaOptOut, false);
            SetAppOptOut(gaOptOut);
        }

        public void TrackAppEvent(string eventName, string label = null)
        {
            TrackEvent("App", eventName, label);
        }

        public void TrackExtensionEvent(string eventName, string label = null)
        {
            TrackEvent("Extension", eventName, label);
        }

        public void TrackEvent(string category, string eventName, string label = null)
        {
            var dict = DictionaryBuilder.CreateEvent(category, eventName, label, null).Build();
            _tracker.Send(dict);
            Gai.SharedInstance.Dispatch();
        }

        public void TrackException(string message, bool fatal)
        {
            var dict = DictionaryBuilder.CreateException(message, fatal).Build();
            _tracker.Send(dict);
        }

        public void TrackPage(string pageName)
        {
            _tracker.Set(GaiConstants.ScreenName, pageName);
            var dict = DictionaryBuilder.CreateScreenView().Build();
            _tracker.Send(dict);
        }

        public void Dispatch(Action completionHandler = null)
        {
            Gai.SharedInstance.Dispatch((result) =>
            {
                completionHandler?.Invoke();
            });
        }

        public void SetAppOptOut(bool optOut)
        {
            Gai.SharedInstance.OptOut = optOut;
        }
    }
}
