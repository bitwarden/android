using System.Collections;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class RepeaterView : StackLayout
    {
        public static readonly BindableProperty ItemTemplateProperty = BindableProperty.Create(
            nameof(ItemTemplate), typeof(DataTemplate), typeof(RepeaterView), default(DataTemplate));

        public static readonly BindableProperty ItemsSourceProperty = BindableProperty.Create(
            nameof(ItemsSource), typeof(ICollection), typeof(RepeaterView), null, BindingMode.OneWay,
            propertyChanged: ItemsChanged);

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

        protected virtual View ViewFor(object item)
        {
            View view = null;
            var template = ItemTemplate;
            if(template != null)
            {
                if(template is DataTemplateSelector selector)
                {
                    template = selector.SelectTemplate(item, this);
                }
                var content = template.CreateContent();
                view = content is View ? content as View : ((ViewCell)content).View;
                view.BindingContext = item;
            }
            return view;
        }

        private static void ItemsChanged(BindableObject bindable, object oldValue, object newValue)
        {
            if(bindable is RepeaterView control)
            {
                control.Children.Clear();
                if(newValue is ICollection items)
                {
                    foreach(var item in items)
                    {
                        control.Children.Add(control.ViewFor(item));
                    }
                }
            }
        }
    }
}
