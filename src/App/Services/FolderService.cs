using System;
using System.Linq;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Data;
using Bit.App.Models.Api;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class FolderService : IFolderService
    {
        private readonly IFolderRepository _folderRepository;
        private readonly IAuthService _authService;
        private readonly IFolderApiRepository _folderApiRepository;

        public FolderService(
            IFolderRepository folderRepository,
            IAuthService authService,
            IFolderApiRepository folderApiRepository)
        {
            _folderRepository = folderRepository;
            _authService = authService;
            _folderApiRepository = folderApiRepository;
        }

        public async Task<Folder> GetByIdAsync(string id)
        {
            var data = await _folderRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var folder = new Folder(data);
            return folder;
        }

        public async Task<IEnumerable<Folder>> GetAllAsync()
        {
            var data = await _folderRepository.GetAllByUserIdAsync(_authService.UserId);
            var folders = data.Select(f => new Folder(f));
            return folders;
        }

        public async Task<ApiResult<FolderResponse>> SaveAsync(Folder folder)
        {
            ApiResult<FolderResponse> response = null;
            var request = new FolderRequest(folder);

            if(folder.Id == null)
            {
                response = await _folderApiRepository.PostAsync(request);
            }
            else
            {
                response = await _folderApiRepository.PutAsync(folder.Id, request);
            }

            if(response.Succeeded)
            {
                var data = new FolderData(response.Result, _authService.UserId);
                if(folder.Id == null)
                {
                    await _folderRepository.InsertAsync(data);
                    folder.Id = data.Id;
                }
                else
                {
                    await _folderRepository.UpdateAsync(data);
                }
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }

        public async Task<ApiResult> DeleteAsync(string folderId)
        {
            var response = await _folderApiRepository.DeleteAsync(folderId);
            if(response.Succeeded)
            {
                await _folderRepository.DeleteAsync(folderId);
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }
    }
}
