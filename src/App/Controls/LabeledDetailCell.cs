using FFImageLoading.Forms;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledDetailCell : ExtendedViewCell
    {
        public LabeledDetailCell()
        {
            Icon = new CachedImage
            {
                WidthRequest = 20,
                HeightRequest = 20,
                HorizontalOptions = LayoutOptions.Center,
                VerticalOptions = LayoutOptions.Center,
                ErrorPlaceholder = "login.png",
                CacheDuration = TimeSpan.FromDays(30),
                BitmapOptimizations = true
            };

            Label = new Label
            {
                LineBreakMode = LineBreakMode.TailTruncation,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            };

            Detail = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                LineBreakMode = LineBreakMode.TailTruncation,
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            LabelIcon = new CachedImage
            {
                WidthRequest = 16,
                HeightRequest = 16,
                HorizontalOptions = LayoutOptions.Start,
                Margin = new Thickness(5, 0, 0, 0)
            };

            LabelIcon2 = new CachedImage
            {
                WidthRequest = 16,
                HeightRequest = 16,
                HorizontalOptions = LayoutOptions.Start,
                Margin = new Thickness(5, 0, 0, 0)
            };

            Button = new ExtendedButton
            {
                WidthRequest = 60
            };

            var grid = new Grid
            {
                ColumnSpacing = 0,
                RowSpacing = 0,
                Padding = new Thickness(3, 3, 0, 3)
            };
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(40, GridUnitType.Absolute) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Auto) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Auto) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(60, GridUnitType.Absolute) });
            grid.Children.Add(Icon, 0, 0);
            grid.Children.Add(Label, 1, 0);
            grid.Children.Add(Detail, 1, 1);
            grid.Children.Add(LabelIcon, 2, 0);
            grid.Children.Add(LabelIcon2, 3, 0);
            grid.Children.Add(Button, 4, 0);
            Grid.SetRowSpan(Icon, 2);
            Grid.SetRowSpan(Button, 2);
            Grid.SetColumnSpan(Detail, 3);

            if(Device.RuntimePlatform == Device.Android)
            {
                Label.TextColor = Color.Black;
            }

            View = grid;
        }

        public CachedImage Icon { get; private set; }
        public Label Label { get; private set; }
        public Label Detail { get; private set; }
        public CachedImage LabelIcon { get; private set; }
        public CachedImage LabelIcon2 { get; private set; }
        public Button Button { get; private set; }
    }
}
