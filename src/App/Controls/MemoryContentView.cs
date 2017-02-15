using Bit.App.Abstractions;
using System;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class MemoryContentView : ContentView
    {
        private readonly IMemoryService _memoryService;

        public MemoryContentView()
        {
            _memoryService = Resolver.Resolve<IMemoryService>();

            var grid = new Grid
            {
                Padding = 5,
                BackgroundColor = Color.White,
                RowDefinitions = new RowDefinitionCollection
                {
                    new RowDefinition { Height = GridLength.Auto },
                    new RowDefinition { Height = GridLength.Auto },
                    new RowDefinition { Height = GridLength.Auto },
                    new RowDefinition { Height = GridLength.Auto },
                    new RowDefinition { Height = GridLength.Auto },
                    new RowDefinition { Height = GridLength.Auto },
                    new RowDefinition { Height = GridLength.Auto }
                },
                ColumnDefinitions = new ColumnDefinitionCollection
                {
                    new ColumnDefinition { Width = GridLength.Star },
                    new ColumnDefinition { Width = GridLength.Star }
                }
            };

            grid.Children.Add(new Label { Text = "Used Memory:" }, 0, 0);
            grid.Children.Add(new Label { Text = "Free Memory:" }, 0, 1);
            grid.Children.Add(new Label { Text = "Heap Memory:" }, 0, 2);
            grid.Children.Add(new Label { Text = "Max Memory:" }, 0, 3);
            grid.Children.Add(new Label { Text = "% Used Heap:" }, 0, 4);
            grid.Children.Add(new Label { Text = "% Used Max:" }, 0, 5);

            UsedMemory = new Label { Text = "Used Memory:", HorizontalTextAlignment = TextAlignment.End };
            FreeMemory = new Label { Text = "Free Memory:", HorizontalTextAlignment = TextAlignment.End };
            HeapMemory = new Label { Text = "Heap Memory:", HorizontalTextAlignment = TextAlignment.End };
            MaxMemory = new Label { Text = "Max Memory:", HorizontalTextAlignment = TextAlignment.End };
            HeapUsage = new Label { Text = "% Used Heap:", HorizontalTextAlignment = TextAlignment.End };
            TotalUsage = new Label { Text = "% Used Max:", HorizontalTextAlignment = TextAlignment.End };

            grid.Children.Add(UsedMemory, 1, 0);
            grid.Children.Add(FreeMemory, 1, 1);
            grid.Children.Add(HeapMemory, 1, 2);
            grid.Children.Add(MaxMemory, 1, 3);
            grid.Children.Add(HeapUsage, 1, 4);
            grid.Children.Add(TotalUsage, 1, 5);

            var button = new ExtendedButton { Text = "Refresh", BackgroundColor = Color.Transparent };
            button.Clicked += Button_Clicked;
            grid.Children.Add(button, 0, 6);
            Grid.SetColumnSpan(button, 2);

            Content = grid;

            RefreshScreen();
        }

        private void Button_Clicked(object sender, EventArgs e)
        {
            RefreshScreen();
        }

        public Label UsedMemory { get; set; }
        public Label FreeMemory { get; set; }
        public Label HeapMemory { get; set; }
        public Label MaxMemory { get; set; }
        public Label HeapUsage { get; set; }
        public Label TotalUsage { get; set; }

        void RefreshScreen()
        {
            UsedMemory.Text = FreeMemory.Text = HeapMemory.Text = MaxMemory.Text =
                HeapUsage.Text = TotalUsage.Text = string.Empty;

            UsedMemory.TextColor = FreeMemory.TextColor = HeapMemory.TextColor =
                MaxMemory.TextColor = HeapUsage.TextColor = TotalUsage.TextColor = Color.Black;

            if(_memoryService != null)
            {
                var info = _memoryService.GetInfo();
                if(info != null)
                {
                    UsedMemory.Text = string.Format("{0:N} mb", Math.Round(info.UsedMemory / 1024 / 1024D, 2));
                    FreeMemory.Text = string.Format("{0:N} mb", Math.Round(info.FreeMemory / 1024 / 1024D, 2));
                    HeapMemory.Text = string.Format("{0:N} mb", Math.Round(info.TotalMemory / 1024 / 1024D, 2));
                    MaxMemory.Text = string.Format("{0:N} mb", Math.Round(info.MaxMemory / 1024 / 1024D, 2));
                    HeapUsage.Text = string.Format("{0:P}", info.HeapUsage());
                    TotalUsage.Text = string.Format("{0:P}", info.Usage());

                    if(info.Usage() > 0.8)
                    {
                        FreeMemory.TextColor = UsedMemory.TextColor = TotalUsage.TextColor = Color.Red;
                    }
                }
            }
        }
    }
}
