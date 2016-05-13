using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class DismissModalToolBarItem : ToolbarItem
    {
        private readonly ContentPage _page;

        public DismissModalToolBarItem(ContentPage page, string text = null)
        {
            _page = page;
            Text = text ?? "Close";
            Clicked += ClickedItem;
            Priority = -1;
        }

        private async void ClickedItem(object sender, EventArgs e)
        {
            await _page.Navigation.PopModalAsync();
        }
    }
}
