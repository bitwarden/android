using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;

namespace Bit.Core.Utilities
{
    public class ExtendedObservableCollection<T> : ObservableCollection<T>
    {
        public ExtendedObservableCollection()
            : base() { }

        public ExtendedObservableCollection(IEnumerable<T> collection)
            : base(collection) { }

        public ExtendedObservableCollection(List<T> list)
            : base(list) { }

        public void AddRange(IEnumerable<T> range)
        {
            foreach (var item in range)
            {
                Items.Add(item);
            }

            OnPropertyChanged(new PropertyChangedEventArgs("Count"));
            OnPropertyChanged(new PropertyChangedEventArgs("Item[]"));
            OnCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        }

        public void ResetWithRange(IEnumerable<T> range)
        {
            // Maybe a fix for https://forums.xamarin.com/discussion/19114/invalid-number-of-rows-in-section
            // Items.Clear();
            if (Items.Count > 0)
            {
                var count = Items.Count;
                for (var i = 0; i < count; i++)
                {
                    Items.RemoveAt(0);
                }
            }
            AddRange(range);
        }
    }
}
