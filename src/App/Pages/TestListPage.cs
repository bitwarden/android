using System;
using Xamarin.Forms;
using Bit.App.Utilities;
using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class TestListPage : ContentPage
    {
        public TestListPage()
        {
            var textCell = new TextCell();
            textCell.SetBinding<Bar>(TextCell.TextProperty, s => s.BarName);

            var listView = new ListView
            {
                IsGroupingEnabled = true,
                GroupDisplayBinding = new Binding("FooName"),
                ItemsSource = Items,
                ItemTemplate = new DataTemplate(() => textCell),
                HasUnevenRows = true,
                SeparatorColor = Color.FromHex("d2d6de")
            };

            Content = listView;
        }

        public ExtendedObservableCollection<Foo> Items { get; private set; } = new ExtendedObservableCollection<Foo>();

        protected override void OnAppearing()
        {
            base.OnAppearing();

            var foos = new List<Foo>();
            foos.Add(new Foo("Foo 1", MakeBars()));
            foos.Add(new Foo("Foo 2", MakeBars()));
            foos.Add(new Foo("Foo 3", MakeBars()));
            foos.Add(new Foo("Foo 4", MakeBars()));
            Items.Reset(foos);
        }

        private IEnumerable<Bar> MakeBars()
        {
            var numbers = new List<Bar>();
            for(int i = 0; i < 100; i++)
            {
                numbers.Add(new Bar { BarName = i.ToString() });
            }
            return numbers;
        }

        public class Foo : List<Bar>
        {
            public Foo(string name, IEnumerable<Bar> bars)
            {
                FooName = name;
                AddRange(bars);
            }

            public string FooName { get; set; }
        }

        public class Bar
        {
            public string BarName { get; set; }
        }
    }
}
