namespace Bit.App.Abstractions
{
    public interface IGoogleAnalyticsService
    {
        void TrackPage(string pageName);
        void TrackEvent(string category, string eventName);
        void TrackException(string message, bool fatal);
    }
}
