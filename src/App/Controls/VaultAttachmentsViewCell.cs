using Bit.App.Models.Page;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class VaultAttachmentsViewCell : LabeledRightDetailCell
    {
        public VaultAttachmentsViewCell()
        {
            Label.SetBinding(Label.TextProperty, nameof(VaultAttachmentsPageModel.Attachment.Name));
            Detail.SetBinding(Label.TextProperty, nameof(VaultAttachmentsPageModel.Attachment.SizeName));
            Icon.Source = "trash";
            Detail.MinimumWidthRequest = 100;
            BackgroundColor = Color.White;

            if(Device.RuntimePlatform == Device.iOS)
            {
                StackLayout.BackgroundColor = Color.White;
            }
        }
    }
}
