using System;
using Bit.App.Pages;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class SendViewCell : ViewCell
    {
        public static readonly BindableProperty SendProperty = BindableProperty.Create(
            nameof(Send), typeof(SendView), typeof(SendViewCell), default(SendView), BindingMode.OneWay);

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(Command<SendView>), typeof(SendViewCell));
        
        public static readonly BindableProperty ShowOptionsProperty = BindableProperty.Create(
            nameof(ShowOptions), typeof(bool), typeof(SendViewCell));

        private readonly IEnvironmentService _environmentService;

        private SendViewCellViewModel _viewModel;
        private bool _usingNativeCell;

        public SendViewCell()
        {
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            if (Device.RuntimePlatform == Device.iOS)
            {
                InitializeComponent();
                _viewModel = _grid.BindingContext as SendViewCellViewModel;
            }
            else
            {
                _usingNativeCell = true;
            }
        }

        public SendView Send
        {
            get => GetValue(SendProperty) as SendView;
            set => SetValue(SendProperty, value);
        }

        public Command<SendView> ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as Command<SendView>;
            set => SetValue(ButtonCommandProperty, value);
        }

        public bool ShowOptions
        {
            get => GetValue(ShowOptionsProperty) is bool && (bool)GetValue(ShowOptionsProperty);
            set => SetValue(ShowOptionsProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (_usingNativeCell)
            {
                return;
            }
            if (propertyName == SendProperty.PropertyName)
            {
                _viewModel.Send = Send;
            }
            else if (propertyName == ShowOptionsProperty.PropertyName)
            {
                _viewModel.ShowOptions = ShowOptions;
            }
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();
            if (_usingNativeCell)
            {
                return;
            }

            SendView send = null;
            if (BindingContext is SendGroupingsPageListItem sendGroupingsPageListItem)
            {
                send = sendGroupingsPageListItem.Send;
            }
            else if (BindingContext is SendView sv)
            {
                send = sv;
            }
            if (send != null)
            {
                var iconImage = GetIconImage(send);
                _icon.IsVisible = true;
                _icon.Text = iconImage;
            }
        }

        public string GetIconImage(SendView send)
        {
            string icon = null;
            switch (send.Type)
            {
                case SendType.Text:
                    icon = "\uf0f6"; // fa-file-text-o
                    break;
                case SendType.File:
                    icon = "\uf016"; // fa-file-o
                    break;
                default:
                    break;
            }
            return icon;
        }

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            ButtonCommand?.Execute(Send);
        }
    }
}
