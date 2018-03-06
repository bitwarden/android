using Bit.App.Abstractions;
using FFImageLoading.Forms;
using System;
using System.Collections.Generic;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class FormEntryCell : ExtendedViewCell, IDisposable
    {
        private VisualElement _nextElement;
        private TapGestureRecognizer _tgr;

        public FormEntryCell(
            string labelText,
            Keyboard entryKeyboard = null,
            bool isPassword = false,
            VisualElement nextElement = null,
            bool useLabelAsPlaceholder = false,
            string imageSource = null,
            Thickness? containerPadding = null,
            bool useButton = false)
        {
            if(!useLabelAsPlaceholder)
            {
                Label = new Label
                {
                    Text = labelText,
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    Style = (Style)Application.Current.Resources["text-muted"],
                    HorizontalOptions = LayoutOptions.FillAndExpand
                };
            }

            Entry = new ExtendedEntry
            {
                Keyboard = entryKeyboard,
                HasBorder = false,
                IsPassword = isPassword,
                AllowClear = true,
                HorizontalOptions = LayoutOptions.FillAndExpand,
                WidthRequest = 1,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Entry))
            };

            if(useLabelAsPlaceholder)
            {
                Entry.Placeholder = labelText;
            }

            NextElement = nextElement;

            var imageStackLayout = new StackLayout
            {
                Padding = containerPadding ?? new Thickness(15, 10),
                Orientation = StackOrientation.Horizontal,
                Spacing = 10,
                HorizontalOptions = LayoutOptions.FillAndExpand,
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            if(imageSource != null)
            {
                _tgr = new TapGestureRecognizer();

                var theImage = new CachedImage
                {
                    Source = imageSource,
                    HorizontalOptions = LayoutOptions.Start,
                    VerticalOptions = LayoutOptions.Center,
                    WidthRequest = 18,
                    HeightRequest = 18
                };
                theImage.GestureRecognizers.Add(_tgr);

                imageStackLayout.Children.Add(theImage);
            }

            var formStackLayout = new StackLayout
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                VerticalOptions = LayoutOptions.CenterAndExpand
            };

            if(Device.RuntimePlatform == Device.Android)
            {
                var deviceInfo = Resolver.Resolve<IDeviceInfoService>();
                if(useLabelAsPlaceholder)
                {
                    if(deviceInfo.Version < 21)
                    {
                        Entry.Margin = new Thickness(-9, 1, -9, 0);
                    }
                    else if(deviceInfo.Version == 21)
                    {
                        Entry.Margin = new Thickness(0, 4, 0, -4);
                    }
                }
                else
                {
                    Entry.AdjustMarginsForDevice();
                }

                if(containerPadding == null)
                {
                    imageStackLayout.AdjustPaddingForDevice();
                }
            }

            if(!useLabelAsPlaceholder)
            {
                formStackLayout.Children.Add(Label);
            }

            formStackLayout.Children.Add(Entry);
            imageStackLayout.Children.Add(formStackLayout);

            if(useButton)
            {
                Button = new ExtendedButton();
                imageStackLayout.Children.Add(Button);

                if(Device.RuntimePlatform == Device.Android)
                {
                    Button.Padding = new Thickness(0);
                    Button.BackgroundColor = Color.Transparent;
                    Button.WidthRequest = 40;
                }
            }

            View = imageStackLayout;
        }

        public Label Label { get; private set; }
        public ExtendedEntry Entry { get; private set; }
        public ExtendedButton Button { get; private set; }
        public VisualElement NextElement
        {
            get => _nextElement;
            set
            {
                _nextElement = value;
                if(_nextElement != null && Entry != null)
                {
                    Entry.ReturnType = Enums.ReturnType.Next;
                }
                else if(Entry != null)
                {
                    Entry.ReturnType = null;
                }
            }
        }
        public Dictionary<string, object> MetaData { get; set; }

        public void InitEvents()
        {
            if(_nextElement != null)
            {
                Entry.Completed += Entry_Completed;
            }

            if(_tgr != null)
            {
                _tgr.Tapped += Tgr_Tapped;
            }

            Tapped += FormEntryCell_Tapped;
        }

        private void Tgr_Tapped(object sender, EventArgs e)
        {
            Entry.Focus();
        }

        private void FormEntryCell_Tapped(object sender, EventArgs e)
        {
            Entry.Focus();
        }

        private void Entry_Completed(object sender, EventArgs e)
        {
            _nextElement?.Focus();
        }

        public void Dispose()
        {
            if(_tgr != null)
            {
                _tgr.Tapped -= Tgr_Tapped;
            }

            Tapped -= FormEntryCell_Tapped;
            Entry.Completed -= Entry_Completed;
        }
    }
}
