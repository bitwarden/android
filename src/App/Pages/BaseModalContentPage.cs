using System;
namespace Bit.App.Pages
{
    public class BaseModalContentPage : BaseContentPage
    {
        public BaseModalContentPage()
        {
        }

        protected void PopModal_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PopModalAsync();
            }
        }
    }
}
