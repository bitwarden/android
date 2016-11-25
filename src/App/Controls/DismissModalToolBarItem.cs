using Bit.App.Resources;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class DismissModalToolBarItem : ToolbarItem
    {
        private readonly ContentPage _page;
        private readonly Action _cancelClickedAction;

        public DismissModalToolBarItem(ContentPage page, string text = null, Action cancelClickedAction = null)
        {
            _cancelClickedAction = cancelClickedAction;
            _page = page;
            Text = text ?? AppResources.Close;
            Clicked += ClickedItem;
            Priority = -1;
        }

        private async void ClickedItem(object sender, EventArgs e)
        {
            _cancelClickedAction?.Invoke();
            await _page.Navigation.PopModalAsync();
        }
    }
}
