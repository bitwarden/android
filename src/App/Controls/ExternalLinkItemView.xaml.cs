using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class ExternalLinkItemView : ContentView
    {
        public static readonly BindableProperty TitleProperty = BindableProperty.Create(
            nameof(Title), typeof(string), typeof(ExternalLinkItemView), null, BindingMode.OneWay);

        public static readonly BindableProperty GoToLinkCommandProperty = BindableProperty.Create(
            nameof(GoToLinkCommand), typeof(ICommand), typeof(ExternalLinkItemView));

        public ExternalLinkItemView ()
        {
            InitializeComponent ();
        }

        public string Title
        {
            get { return (string)GetValue(TitleProperty); }
            set { SetValue(TitleProperty, value); }
        }

        public ICommand GoToLinkCommand
        {
            get => GetValue(GoToLinkCommandProperty) as ICommand;
            set => SetValue(GoToLinkCommandProperty, value);
        }
    }
}
