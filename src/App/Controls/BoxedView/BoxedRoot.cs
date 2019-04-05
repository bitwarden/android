using System;
using System.Collections.Specialized;
using System.ComponentModel;
using Xamarin.Forms;

namespace Bit.App.Controls.BoxedView
{
    public class BoxedRoot : TableSectionBase<BoxedSection>
    {
        public BoxedRoot()
        {
            SetupEvents();
        }

        public event EventHandler<EventArgs> SectionCollectionChanged;

        private void ChildCollectionChanged(object sender, NotifyCollectionChangedEventArgs args)
        {
            SectionCollectionChanged?.Invoke(this, EventArgs.Empty);
        }

        private void ChildPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if(e.PropertyName == TitleProperty.PropertyName)
            {
                OnPropertyChanged(TitleProperty.PropertyName);
            }
            else if(e.PropertyName == BoxedSection.FooterTextProperty.PropertyName)
            {
                OnPropertyChanged(BoxedSection.FooterTextProperty.PropertyName);
            }
            else if(e.PropertyName == BoxedSection.IsVisibleProperty.PropertyName)
            {
                OnPropertyChanged(BoxedSection.IsVisibleProperty.PropertyName);
            }
        }

        private void SetupEvents()
        {
            CollectionChanged += (sender, args) =>
            {
                if(args.NewItems != null)
                {
                    foreach(BoxedSection section in args.NewItems)
                    {
                        section.CollectionChanged += ChildCollectionChanged;
                        section.PropertyChanged += ChildPropertyChanged;
                    }
                }
                if(args.OldItems != null)
                {
                    foreach(BoxedSection section in args.OldItems)
                    {
                        section.CollectionChanged -= ChildCollectionChanged;
                        section.PropertyChanged -= ChildPropertyChanged;
                    }
                }
            };
        }
    }
}
