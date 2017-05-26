using System;
using Bit.App;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using Android.Gms.Analytics;
using Android.Content;

namespace Bit.Android.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        private readonly GoogleAnalytics _instance;
        private readonly IAuthService _authService;
        private readonly Tracker _tracker;

        public GoogleAnalyticsService(
            Context appContext,
            IAppIdService appIdService,
            IAuthService authService,
            ISettings settings)
        {
            _authService = authService;

            _instance = GoogleAnalytics.GetInstance(appContext.ApplicationContext);
            _instance.SetLocalDispatchPeriod(10);

            _tracker = _instance.NewTracker("UA-81915606-2");
            _tracker.EnableExceptionReporting(false);
            _tracker.EnableAdvertisingIdCollection(true);
            _tracker.EnableAutoActivityTracking(true);
            _tracker.SetClientId(appIdService.AnonymousAppId);

            var gaOptOut = settings.GetValueOrDefault(Constants.SettingGaOptOut, false);
            SetAppOptOut(gaOptOut);
        }

        public void TrackAppEvent(string eventName, string label = null)
        {
            TrackEvent("App", eventName, label);
        }

        public void TrackExtensionEvent(string eventName, string label = null)
        {
            TrackEvent("AutofillService", eventName, label);
        }

        public void TrackEvent(string category, string eventName, string label = null)
        {
            var builder = new HitBuilders.EventBuilder();
            builder.SetCategory(category);
            builder.SetAction(eventName);
            if(label != null)
            {
                builder.SetLabel(label);
            }

            _tracker.Send(builder.Build());
        }

        public void TrackException(string message, bool fatal)
        {
            var builder = new HitBuilders.ExceptionBuilder();
            builder.SetDescription(message);
            builder.SetFatal(fatal);

            _tracker.Send(builder.Build());
        }

        public void TrackPage(string pageName)
        {
            _tracker.SetScreenName(pageName);
            _tracker.Send(new HitBuilders.ScreenViewBuilder().Build());
        }

        public void Dispatch(Action completionHandler = null)
        {
            _instance.DispatchLocalHits();
            completionHandler?.Invoke();
        }

        public void SetAppOptOut(bool optOut)
        {
            _instance.AppOptOut = optOut;
        }
    }
}
