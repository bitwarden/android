using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public class DateTimeViewModel : ExtendedViewModel
    {
        DateTime? _date;
        TimeSpan? _time;

        public DateTimeViewModel(string dateName, string timeName)
        {
            DateName = dateName;
            TimeName = timeName;
        }

        public Action<DateTime?> OnDateChanged { get; set; }
        public Action<TimeSpan?> OnTimeChanged { get; set; }

        public DateTime? Date
        {
            get => _date;
            set
            {
                if (SetProperty(ref _date, value))
                {
                    OnDateChanged?.Invoke(value);
                }
            }
        }
        public TimeSpan? Time
        {
            get => _time;
            set
            {
                if (SetProperty(ref _time, value))
                {
                    OnTimeChanged?.Invoke(value);
                }
            }
        }

        public string DateName { get; }
        public string TimeName { get; }

        public string DatePlaceholder { get; set; }
        public string TimePlaceholder { get; set; }

        public DateTime? DateTime
        {
            get
            {
                if (Date.HasValue)
                {
                    if (Time.HasValue)
                    {
                        return Date.Value.Add(Time.Value);
                    }
                    return Date;
                }
                return null;
            }
            set
            {
                Date = value?.Date;
                Time = value?.Date.TimeOfDay;
            }
        }
    }
}
