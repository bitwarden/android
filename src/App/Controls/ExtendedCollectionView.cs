using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedCollectionView : CollectionView
    {
        public static BindableProperty OnSwipeCommandProperty =
            BindableProperty.Create(nameof(OnSwipeCommand), typeof(ICommand), typeof(ExtendedCollectionView));

        public ICommand OnSwipeCommand
        {
            get => (ICommand)GetValue(OnSwipeCommandProperty);
            set => SetValue(OnSwipeCommandProperty, value);
        }

        public string ExtraDataForLogging { get; set; }
    }
}
