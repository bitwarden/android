using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedListView : ListView
    {
        public ExtendedListView() { }

        public ExtendedListView(ListViewCachingStrategy cachingStrategy)
            : base(cachingStrategy) { }
    }
}
