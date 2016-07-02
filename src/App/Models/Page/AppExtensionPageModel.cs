using System;
using System.ComponentModel;
using Plugin.Settings.Abstractions;

namespace Bit.App.Models.Page
{
    public class AppExtensionPageModel : INotifyPropertyChanged
    {
        private readonly ISettings _settings;

        public AppExtensionPageModel(ISettings settings)
        {
            _settings = settings;
        }

        public event PropertyChangedEventHandler PropertyChanged;

        public bool Started
        {
            get { return _settings.GetValueOrDefault(Constants.ExtensionStarted, false); }
            set
            {
                _settings.AddOrUpdateValue(Constants.ExtensionStarted, true);
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Started)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(NotStarted)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(StartedAndNotActivated)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(StartedAndActivated)));
            }
        }

        public bool Activated
        {
            get { return _settings.GetValueOrDefault(Constants.ExtensionActivated, false); }
            set
            {
                _settings.AddOrUpdateValue(Constants.ExtensionActivated, value);
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Activated)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(StartedAndNotActivated)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(StartedAndActivated)));
            }
        }

        public bool NotStarted => !Started;
        public bool StartedAndNotActivated => Started && !Activated;
        public bool StartedAndActivated => Started && Activated;
    }
}
