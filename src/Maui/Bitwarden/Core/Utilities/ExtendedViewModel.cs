using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Bit.Core.Utilities
{
    public abstract class ExtendedViewModel : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        protected bool SetProperty<T>(ref T backingStore, T value, Action onChanged = null,
            [CallerMemberName] string propertyName = "", string[] additionalPropertyNames = null)
        {
            if (EqualityComparer<T>.Default.Equals(backingStore, value))
            {
                return false;
            }

            backingStore = value;
            TriggerPropertyChanged(propertyName, additionalPropertyNames);
            onChanged?.Invoke();
            return true;
        }

        protected void TriggerPropertyChanged(string propertyName, string[] additionalPropertyNames = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            if (PropertyChanged != null && additionalPropertyNames != null)
            {
                foreach (var prop in additionalPropertyNames)
                {
                    PropertyChanged.Invoke(this, new PropertyChangedEventArgs(prop));
                }
            }
        }
    }
}
