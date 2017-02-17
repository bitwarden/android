using Bit.App.Resources;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class DismissModalToolBarItem : ExtendedToolbarItem, IDisposable
    {
        private readonly ContentPage _page;

        public DismissModalToolBarItem(ContentPage page, string text = null, Action cancelClickedAction = null)
            : base(cancelClickedAction)
        {
            _page = page;
            // TODO: init and dispose events from pages
            InitEvents();
            Text = text ?? AppResources.Close;
            Priority = -1;
        }

        protected async override void ClickedItem(object sender, EventArgs e)
        {
            base.ClickedItem(sender, e);
            await _page.Navigation.PopModalAsync();
        }
    }
}
