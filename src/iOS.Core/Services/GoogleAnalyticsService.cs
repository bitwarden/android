using System;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.iOS.Core.Services
{
    public class GoogleAnalyticsService : IGoogleAnalyticsService
    {
        public GoogleAnalyticsService(
            IAppIdService appIdService,
            ISettings settings)
        {}

        public void TrackAppEvent(string eventName, string label = null)
        {
        }

        public void TrackExtensionEvent(string eventName, string label = null)
        {
        }

        public void TrackAutofillExtensionEvent(string eventName, string label = null)
        {
        }

        public void TrackEvent(string category, string eventName, string label = null)
        {
        }

        public void TrackException(string message, bool fatal)
        {
        }

        public void TrackPage(string pageName)
        {
        }

        public void Dispatch(Action completionHandler = null)
        {
        }

        public void SetAppOptOut(bool optOut)
        {
        }
    }
}
