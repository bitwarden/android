using System;
using Bit.App.Resources;

namespace Bit.App.Utilities
{
    public static class A11yExtensions
    {
        public enum TimeSpanVerbalizationMode
        {
            HoursAndMinutes,
            Hours
        }

        public static string Verbalize(this TimeSpan timeSpan, TimeSpanVerbalizationMode mode)
        {
            if (mode == TimeSpanVerbalizationMode.Hours)
            {
                if (timeSpan.TotalHours == 1)
                {
                    return AppResources.OneHour;
                }

                return string.Format(AppResources.XHours, timeSpan.TotalHours);
            }

            if (timeSpan.Hours == 1)
            {
                if (timeSpan.Minutes == 1)
                {
                    return AppResources.OneHourAndOneMinute;
                }
                return string.Format(AppResources.OneHourAndXMinute, timeSpan.Minutes);
            }

            if (timeSpan.Minutes == 1)
            {
                return string.Format(AppResources.XHoursAndOneMinute, timeSpan.Hours);
            }

            return string.Format(AppResources.XHoursAndYMinutes, timeSpan.Hours, timeSpan.Minutes);
        }
    }
}
