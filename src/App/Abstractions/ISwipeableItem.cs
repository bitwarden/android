using Xamarin.Forms;

namespace Bit.App.Abstractions
{
    public interface ISwipeableItem<TItem>
    {
        bool CanSwipe(TItem item);
        FontImageSource GetSwipeIcon(TItem item);
        Color GetBackgroundColor(TItem item);
    }
}
