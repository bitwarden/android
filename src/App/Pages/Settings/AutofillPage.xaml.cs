using System;

namespace Bit.App.Pages
{
    public partial class AutofillPage : BaseContentPage
    {
        public AutofillPage()
        {
            InitializeComponent();
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PopModalAsync();
            }
        }
    }
}
