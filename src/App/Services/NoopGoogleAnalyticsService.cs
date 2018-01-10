using System;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    public class NoopGoogleAnalyticsService : IGoogleAnalyticsService
    {
        public void TrackAppEvent(string eventName, string label = null)
        {
        }

        public void TrackExtensionEvent(string eventName, string label = null)
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
