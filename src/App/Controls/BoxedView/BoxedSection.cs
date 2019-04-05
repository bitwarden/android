using System.Collections;
using System.Collections.Specialized;
using Xamarin.Forms;

namespace Bit.App.Controls.BoxedView
{
    public class BoxedSection : TableSectionBase<Cell>
    {
        public static BindableProperty IsVisibleProperty = BindableProperty.Create(
            nameof(IsVisible), typeof(bool), typeof(BoxedSection), true, defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty FooterTextProperty = BindableProperty.Create(
            nameof(FooterText), typeof(string), typeof(BoxedSection), default(string),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ItemTemplateProperty = BindableProperty.Create(
            nameof(ItemTemplate), typeof(DataTemplate), typeof(BoxedSection), default(DataTemplate),
            defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty ItemsSourceProperty = BindableProperty.Create(
            nameof(ItemsSource), typeof(IList), typeof(BoxedSection), default(IList),
            defaultBindingMode: BindingMode.OneWay, propertyChanged: ItemsChanged);

        public static BindableProperty HeaderHeightProperty = BindableProperty.Create(
            nameof(HeaderHeight), typeof(double), typeof(BoxedSection), -1d, defaultBindingMode: BindingMode.OneWay);

        public static BindableProperty UseDragSortProperty = BindableProperty.Create(
           nameof(UseDragSort), typeof(bool), typeof(BoxedSection), false, defaultBindingMode: BindingMode.OneWay);

        public BoxedSection()
        { }

        public BoxedSection(string title)
            : base(title)
        { }

        public bool IsVisible
        {
            get => (bool)GetValue(IsVisibleProperty);
            set => SetValue(IsVisibleProperty, value);
        }

        public string FooterText
        {
            get => (string)GetValue(FooterTextProperty);
            set => SetValue(FooterTextProperty, value);
        }

        public DataTemplate ItemTemplate
        {
            get => (DataTemplate)GetValue(ItemTemplateProperty);
            set => SetValue(ItemTemplateProperty, value);
        }

        public IList ItemsSource
        {
            get => (IList)GetValue(ItemsSourceProperty);
            set => SetValue(ItemsSourceProperty, value);
        }

        public double HeaderHeight
        {
            get => (double)GetValue(HeaderHeightProperty);
            set => SetValue(HeaderHeightProperty, value);
        }

        public bool UseDragSort
        {
            get => (bool)GetValue(UseDragSortProperty);
            set => SetValue(UseDragSortProperty, value);
        }

        private static void ItemsChanged(BindableObject bindable, object oldValue, object newValue)
        {
            var section = bindable as BoxedSection;
            if(section.ItemTemplate == null)
            {
                return;
            }

            if(oldValue is INotifyCollectionChanged oldObservableCollection)
            {
                oldObservableCollection.CollectionChanged -= section.OnItemsSourceCollectionChanged;
            }
            if(newValue is INotifyCollectionChanged newObservableCollection)
            {
                newObservableCollection.CollectionChanged += section.OnItemsSourceCollectionChanged;
            }

            section.Clear();

            if(newValue is IList newValueAsEnumerable)
            {
                foreach(var item in newValueAsEnumerable)
                {
                    var view = CreateChildViewFor(section.ItemTemplate, item, section);
                    section.Add(view);
                }
            }
        }

        private void OnItemsSourceCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            if(e.Action == NotifyCollectionChangedAction.Replace)
            {
                RemoveAt(e.OldStartingIndex);
                var item = e.NewItems[e.NewStartingIndex];
                var view = CreateChildViewFor(ItemTemplate, item, this);
                Insert(e.NewStartingIndex, view);
            }
            else if(e.Action == NotifyCollectionChangedAction.Add)
            {
                if(e.NewItems != null)
                {
                    for(var i = 0; i < e.NewItems.Count; ++i)
                    {
                        var item = e.NewItems[i];
                        var view = CreateChildViewFor(ItemTemplate, item, this);
                        Insert(i + e.NewStartingIndex, view);
                    }
                }
            }
            else if(e.Action == NotifyCollectionChangedAction.Remove)
            {
                if(e.OldItems != null)
                {
                    RemoveAt(e.OldStartingIndex);
                }
            }
            else if(e.Action == NotifyCollectionChangedAction.Reset)
            {
                Clear();
            }
        }

        private static Cell CreateChildViewFor(DataTemplate template, object item, BindableObject container)
        {
            if(template is DataTemplateSelector selector)
            {
                template = selector.SelectTemplate(item, container);
            }
            // Binding context
            template.SetValue(BindingContextProperty, item);
            return template.CreateContent() as Cell;
        }
    }
}
