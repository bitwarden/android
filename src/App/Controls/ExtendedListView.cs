using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedListView : ListView
    {
        public static readonly BindableProperty BottomPaddingProperty =
            BindableProperty.Create(nameof(BottomPadding), typeof(int), typeof(ExtendedTableView), 0);

        public ExtendedListView() { }
        public ExtendedListView(ListViewCachingStrategy cachingStrategy)
            : base(cachingStrategy) { }

        public int BottomPadding { get; set; }
    }
}
