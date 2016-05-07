using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;

using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class VaultEditSitePage : ContentPage
    {
        public VaultEditSitePage(string siteId)
        {
            Title = "Edit Site " + siteId;
            Content = null;
        }
    }
}
