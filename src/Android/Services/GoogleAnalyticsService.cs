using System;
using Bit.App.Abstractions;
using Android.Gms.Analytics;
using Android.Content;

namespace Bit.Android.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        private const string UserId = "&uid";

        private readonly IAuthService _authService;
        private readonly Tracker _tracker;
        private bool _setUserId = true;

        public GoogleAnalyticsService(
            Context appContext,
            IAppIdService appIdService,
            IAuthService authService)
        {
            _authService = authService;

            var instance = GoogleAnalytics.GetInstance(appContext.ApplicationContext);
            instance.SetLocalDispatchPeriod(10);

            _tracker = instance.NewTracker("UA-81915606-2");
            _tracker.EnableExceptionReporting(true);
            _tracker.EnableAdvertisingIdCollection(true);
            _tracker.EnableAutoActivityTracking(true);
            _tracker.SetClientId(appIdService.AppId);
        }

        public void RefreshUserId()
        {
            _tracker.Set(UserId, null);
            _setUserId = true;
        }

        public void TrackEvent(string category, string eventName)
        {
            var builder = new HitBuilders.EventBuilder();
            builder.SetCategory(category);
            builder.SetAction(eventName);
            builder.SetLabel("AppEvent");

            SetUserId();
            _tracker.Send(builder.Build());
        }

        public void TrackException(string message, bool fatal)
        {
            var builder = new HitBuilders.ExceptionBuilder();
            builder.SetDescription(message);
            builder.SetFatal(fatal);

            SetUserId();
            _tracker.Send(builder.Build());
        }

        public void TrackPage(string pageName)
        {
            SetUserId();
            _tracker.SetScreenName(pageName);
            _tracker.Send(new HitBuilders.ScreenViewBuilder().Build());
        }

        private void SetUserId()
        {
            if(_setUserId && _authService.IsAuthenticated)
            {
                _tracker.Set(UserId, _authService.UserId);
                _setUserId = false;
            }
        }
    }
}
