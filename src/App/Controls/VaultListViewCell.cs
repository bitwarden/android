using Bit.App.Models.Page;
using System;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class VaultListViewCell : LabeledDetailCell
    {
        public static readonly BindableProperty CipherParameterProperty = BindableProperty.Create(nameof(CipherParameter),
            typeof(VaultListPageModel.Cipher), typeof(VaultListViewCell), null);
        public static readonly BindableProperty GroupingOrCipherParameterProperty =
            BindableProperty.Create(nameof(GroupingOrCipherParameter), typeof(VaultListPageModel.GroupingOrCipher),
                typeof(VaultListViewCell), null);

        public VaultListViewCell(Action<VaultListPageModel.Cipher> moreClickedAction, bool groupingOrCipherBinding = false)
        {
            string bindingPrefix = null;
            if(groupingOrCipherBinding)
            {
                SetBinding(GroupingOrCipherParameterProperty, new Binding("."));
                bindingPrefix = string.Concat(nameof(VaultListPageModel.GroupingOrCipher.Cipher), ".");
                Button.Command = new Command(() => moreClickedAction?.Invoke(GroupingOrCipherParameter?.Cipher));
            }
            else
            {
                SetBinding(CipherParameterProperty, new Binding("."));
                Button.Command = new Command(() => moreClickedAction?.Invoke(CipherParameter));
            }

            Label.SetBinding(Label.TextProperty, bindingPrefix + nameof(VaultListPageModel.Cipher.Name));
            Detail.SetBinding(Label.TextProperty, bindingPrefix + nameof(VaultListPageModel.Cipher.Subtitle));
            LabelIcon.SetBinding(VisualElement.IsVisibleProperty,
                bindingPrefix + nameof(VaultListPageModel.Cipher.Shared));
            LabelIcon2.SetBinding(VisualElement.IsVisibleProperty,
                bindingPrefix + nameof(VaultListPageModel.Cipher.HasAttachments));

            Button.Image = "more.png";
            Button.BackgroundColor = Color.Transparent;

            LabelIcon.Source = "share.png";
            LabelIcon2.Source = "paperclip.png";

            BackgroundColor = Color.White;
        }

        public VaultListPageModel.Cipher CipherParameter
        {
            get { return GetValue(CipherParameterProperty) as VaultListPageModel.Cipher; }
            set { SetValue(CipherParameterProperty, value); }
        }

        public VaultListPageModel.GroupingOrCipher GroupingOrCipherParameter
        {
            get { return GetValue(GroupingOrCipherParameterProperty) as VaultListPageModel.GroupingOrCipher; }
            set { SetValue(GroupingOrCipherParameterProperty, value); }
        }

        protected override void OnBindingContextChanged()
        {
            Icon.Source = null;

            VaultListPageModel.Cipher cipher = null;
            if(BindingContext is VaultListPageModel.Cipher item)
            {
                cipher = item;
            }
            else if(BindingContext is VaultListPageModel.GroupingOrCipher groupingOrCipherItem)
            {
                cipher = groupingOrCipherItem.Cipher;
            }

            if(cipher != null)
            {
                if(cipher.Type == Enums.CipherType.Login)
                {
                    Icon.LoadingPlaceholder = "login.png";
                }
                Icon.Source = cipher.Icon;
            }

            base.OnBindingContextChanged();
        }
    }
}
