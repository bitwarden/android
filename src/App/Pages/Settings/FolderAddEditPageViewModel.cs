using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class FolderAddEditPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IFolderService _folderService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private FolderView _folder;

        public FolderAddEditPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");

            SubmitCommand = new Command(async () => await SubmitAsync());
        }

        public Command SubmitCommand { get; }
        public string FolderId { get; set; }
        public FolderView Folder
        {
            get => _folder;
            set => SetProperty(ref _folder, value);
        }
        public bool EditMode => !string.IsNullOrWhiteSpace(FolderId);

        public void Init()
        {
            PageTitle = EditMode ? AppResources.EditFolder : AppResources.AddFolder;
        }

        public async Task LoadAsync()
        {
            if (Folder == null)
            {
                if (EditMode)
                {
                    var folder = await _folderService.GetAsync(FolderId);
                    if (folder != null)
                    {
                        Folder = await folder.DecryptAsync();
                    }
                }
                else
                {
                    Folder = new FolderView();
                }
            }
        }

        public async Task<bool> SubmitAsync()
        {
            if (Folder == null)
            {
                return false;
            }
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            if (string.IsNullOrWhiteSpace(Folder.Name))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.Name),
                    AppResources.Ok);
                return false;
            }

            var folder = await _folderService.EncryptAsync(Folder);
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                await _folderService.SaveWithServerAsync(folder);
                Folder.Id = folder.Id;
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null,
                    EditMode ? AppResources.FolderUpdated : AppResources.FolderCreated);
                await Page.Navigation.PopModalAsync();
                return true;
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            return false;
        }

        public async Task<bool> DeleteAsync()
        {
            if (Folder == null)
            {
                return false;
            }
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.DoYouReallyWantToDelete,
                null, AppResources.Yes, AppResources.No);
            if (!confirmed)
            {
                return false;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Deleting);
                await _folderService.DeleteWithServerAsync(Folder.Id);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.FolderDeleted);
                await Page.Navigation.PopModalAsync();
                return true;
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            return false;
        }
    }
}
