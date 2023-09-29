using System;
using Bit.App.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Controls
{
    public partial class SendViewCell : ExtendedGrid
    {
        public static readonly BindableProperty SendProperty = BindableProperty.Create(
            nameof(Send), typeof(SendView), typeof(SendViewCell), default(SendView), BindingMode.OneWay);

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(Command<SendView>), typeof(SendViewCell));

        public static readonly BindableProperty ShowOptionsProperty = BindableProperty.Create(
            nameof(ShowOptions), typeof(bool), typeof(SendViewCell), true, BindingMode.OneWay);

        public SendViewCell()
        {
            InitializeComponent();

            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _iconColumn.Width = new GridLength(40 * deviceActionService.GetSystemFontSizeScale(), GridUnitType.Absolute);
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
            get => (bool)GetValue(ShowOptionsProperty);
            set => SetValue(ShowOptionsProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);
            if (propertyName == SendProperty.PropertyName)
            {
                if (Send == null)
                {
                    return;
                }
                BindingContext = new SendViewCellViewModel(Send, ShowOptions);
            }
        }

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            var send = ((sender as MiButton)?.BindingContext as SendViewCellViewModel)?.Send;
            if (send != null)
            {
                ButtonCommand?.Execute(send);
            }
        }
    }
}
