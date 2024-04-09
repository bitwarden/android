using System.Windows.Input;

namespace Bit.App.Controls
{
    public partial class ExternalLinkSubtitleItemView : BaseSettingItemView
    {
        public static readonly BindableProperty GoToLinkCommandProperty = BindableProperty.Create(
            nameof(GoToLinkCommand), typeof(ICommand), typeof(ExternalLinkSubtitleItemView));

        public ExternalLinkSubtitleItemView()
        {
            InitializeComponent();
        }

        public ICommand GoToLinkCommand
        {
            get => GetValue(GoToLinkCommandProperty) as ICommand;
            set => SetValue(GoToLinkCommandProperty, value);
        }

        void ContentView_Tapped(System.Object sender, System.EventArgs e)
        {
            GoToLinkCommand?.Execute(null);
        }
    }
}
