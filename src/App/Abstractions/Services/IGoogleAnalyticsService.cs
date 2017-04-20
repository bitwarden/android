using System;

namespace Bit.App.Abstractions
{
    public interface IGoogleAnalyticsService
    {
        void TrackPage(string pageName);
        void TrackAppEvent(string eventName, string label = null);
        void TrackExtensionEvent(string eventName, string label = null);
        void TrackEvent(string category, string eventName, string label = null);
        void TrackException(string message, bool fatal);
        void Dispatch(Action completionHandler = null);
        void SetAppOptOut(bool optOut);
    }
}
