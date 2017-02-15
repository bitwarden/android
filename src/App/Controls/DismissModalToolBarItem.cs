using Bit.App.Resources;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class DismissModalToolBarItem : ToolbarItem, IDisposable
    {
        private readonly ContentPage _page;
        private readonly Action _cancelClickedAction;

        public DismissModalToolBarItem(ContentPage page, string text = null, Action cancelClickedAction = null)
        {
            _cancelClickedAction = cancelClickedAction;
            _page = page;
            // TODO: init and dispose events from pages
            InitEvents();
            Text = text ?? AppResources.Close;
            Priority = -1;
        }

        private async void ClickedItem(object sender, EventArgs e)
        {
            _cancelClickedAction?.Invoke();
            await _page.Navigation.PopModalAsync();
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
