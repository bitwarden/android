using Bit.App.Pages;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class CipherViewCell : Grid
    {
        public static readonly BindableProperty CipherProperty = BindableProperty.Create(
            nameof(Cipher), typeof(CipherView), typeof(CipherViewCell), default(CipherView), BindingMode.OneWay);

        public static readonly BindableProperty WebsiteIconsEnabledProperty = BindableProperty.Create(
            nameof(WebsiteIconsEnabled), typeof(bool), typeof(CipherViewCell), true, BindingMode.OneWay);

        public static readonly BindableProperty ButtonCommandProperty = BindableProperty.Create(
            nameof(ButtonCommand), typeof(Command<CipherView>), typeof(CipherViewCell));

       

        public CipherViewCell()
        {
            

            InitializeComponent();
        }

        public bool WebsiteIconsEnabled
        {
            get => (bool)GetValue(WebsiteIconsEnabledProperty);
            set => SetValue(WebsiteIconsEnabledProperty, value);
        }

        public CipherView Cipher
        {
            get => GetValue(CipherProperty) as CipherView;
            set => SetValue(CipherProperty, value);
        }

        public Command<CipherView> ButtonCommand
        {
            get => GetValue(ButtonCommandProperty) as Command<CipherView>;
            set => SetValue(ButtonCommandProperty, value);
        }

        protected override void OnPropertyChanged(string propertyName = null)
        {
            base.OnPropertyChanged(propertyName);

            if (propertyName == CipherProperty.PropertyName)
            {
                if (Cipher == null)
                    return;

               BindingContext = new CipherViewCellViewModel(Cipher);
            }
        }

        protected override void OnBindingContextChanged()
        {
            base.OnBindingContextChanged();

            //_image.Source = null;
            //CipherView cipher = null;
            //if (BindingContext is GroupingsPageListItem groupingsPageListItem)
            //{
            //    //cipher = groupingsPageListItem.Cipher;
            //}
            //else if (BindingContext is CipherView cv)
            //{
            //    //cipher = cv;
            //}
            //if (cipher != null)
            //{
            //    var iconImage = GetIconImage(cipher);
            //    if (iconImage.Item2 != null)
            //    {
            //        _image.IsVisible = true;
            //        //_icon.IsVisible = false;
            //        _image.Source = iconImage.Item2;
            //        //_image.LoadingPlaceholder = "login.png";
            //    }
            //    else
            //    {
            //        _image.IsVisible = false;
            //        //_icon.IsVisible = true;
            //        //_icon.Text = iconImage.Item1;
            //    }
            //}
        }

        

        

        private void MoreButton_Clicked(object sender, EventArgs e)
        {
            ButtonCommand?.Execute(Cipher);
        }
    }
}
