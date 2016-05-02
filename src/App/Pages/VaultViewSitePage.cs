using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;

using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class VaultViewSitePage : ContentPage
    {
        private int _siteId;

        public VaultViewSitePage(int siteId)
        {
            _siteId = siteId;

            Title = "View Site";
            Content = null;
        }
    }
}
