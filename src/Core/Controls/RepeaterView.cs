using System;
using System.Collections;
using System.Collections.Specialized;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    [Obsolete]
    public class RepeaterView : StackLayout
    {
        public static readonly BindableProperty ItemTemplateProperty = BindableProperty.Create(
            nameof(ItemTemplate), typeof(DataTemplate), typeof(RepeaterView), default(DataTemplate));

        public static readonly BindableProperty ItemsSourceProperty = BindableProperty.Create(
            nameof(ItemsSource), typeof(ICollection), typeof(RepeaterView), null, BindingMode.OneWay,
            propertyChanged: ItemsSourceChanging);

        public RepeaterView()
        {
            Spacing = 0;
        }

        public ICollection ItemsSource
        {
            get => GetValue(ItemsSourceProperty) as ICollection;
            set => SetValue(ItemsSourceProperty, value);
        }

        public DataTemplate ItemTemplate
        {
            get => GetValue(ItemTemplateProperty) as DataTemplate;
            set => SetValue(ItemTemplateProperty, value);
        }

        private void OnCollectionChanged(object sender,
            NotifyCollectionChangedEventArgs notifyCollectionChangedEventArgs)
        {
            Populate();
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == ItemTemplateProperty.PropertyName || propertyName == ItemsSourceProperty.PropertyName)
            {
                Populate();
            }
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();
            Populate();
        }

        protected virtual View ViewFor(object item)
        {
            View view = null;
            var template = ItemTemplate;
            if (template != null)
            {
                if (template is DataTemplateSelector selector)
                {
                    template = selector.SelectTemplate(item, this);
                }
                var content = template.CreateContent();
                view = content is View ? content as View : ((ViewCell)content).View;
                view.BindingContext = item;
            }
            return view;
        }

        private void Populate()
        {
            if (ItemsSource != null)
            {
                Children.Clear();
                foreach (var item in ItemsSource)
                {
                    Children.Add(ViewFor(item));
                }
            }
        }

        private static void ItemsSourceChanging(BindableObject bindable, object oldValue, object newValue)
        {
            if (oldValue != null && oldValue is INotifyCollectionChanged ov)
            {
                ov.CollectionChanged -= (bindable as RepeaterView).OnCollectionChanged;
            }
            if (newValue != null && newValue is INotifyCollectionChanged nv)
            {
                nv.CollectionChanged += (bindable as RepeaterView).OnCollectionChanged;
            }
        }
    }
}
