using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class ExtendedToolbarItem : ToolbarItem, IDisposable
    {
        public ExtendedToolbarItem(Action clickAction = null)
        {
            ClickAction = clickAction;
        }

        public Action ClickAction { get; set; }

        protected virtual void ClickedItem(object sender, EventArgs e)
        {
            ClickAction?.Invoke();
        }

        public void InitEvents()
        {
            Clicked += ClickedItem;
        }

        public void Dispose()
        {
            Clicked -= ClickedItem;
        }
    }
}
