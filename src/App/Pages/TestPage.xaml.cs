using System;
using System.Collections.Generic;

using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class TestPage : BaseContentPage
    {
        public TestPage()
        {
            InitializeComponent();
            BindingContext = new TestPageViewModel();
        }
    }
}
