using Bit.App.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.App.Controls
{
    public partial class SendViewCell : ExtendedGrid
    {
        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(Command<SendView>), typeof(SendViewCell));

        public SendViewCell()
        {
            InitializeComponent();

            var deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _iconColumn.Width = new GridLength(40 * deviceActionService.GetSystemFontSizeScale(), GridUnitType.Absolute);
        }

        public Command<SendView> ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as Command<SendView>;
            set => SetValue(ButtonCommandProperty, value);
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
