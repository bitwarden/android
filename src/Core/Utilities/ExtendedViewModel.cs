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
            [CallerMemberName]string propertyName = "", string[] additionalPropertyNames = null)
        {
            if(EqualityComparer<T>.Default.Equals(backingStore, value))
            {
                return false;
            }

            backingStore = value;
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            if(PropertyChanged != null && additionalPropertyNames != null)
            {
                foreach(var prop in additionalPropertyNames)
                {
                    PropertyChanged.Invoke(this, new PropertyChangedEventArgs(prop));
                }
            }
            onChanged?.Invoke();
            return true;
        }
    }
}
