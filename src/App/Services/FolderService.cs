using System;
using System.Linq;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Data;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using System.Net.Http;
using System.Text;

namespace Bit.App.Services
{
    public class FolderService : Repository<FolderData, string>, IFolderService
    {
        private readonly IAuthService _authService;
        private readonly IApiService _apiService;

        public FolderService(
            ISqlService sqlService,
            IAuthService authService,
            IApiService apiService)
            : base(sqlService)
        {
            _authService = authService;
            _apiService = apiService;
        }

        public new Task<Folder> GetByIdAsync(string id)
        {
            var data = Connection.Table<FolderData>().Where(f => f.UserId == _authService.UserId && f.Id == id).FirstOrDefault();
            return Task.FromResult(new Folder(data));
        }

        public new Task<IEnumerable<Folder>> GetAllAsync()
        {
            var data = Connection.Table<FolderData>().Where(f => f.UserId == _authService.UserId).Cast<FolderData>();
            return Task.FromResult(data.Select(f => new Folder(f)));
        }

        public async Task<ApiResult<FolderResponse>> SaveAsync(Folder folder)
        {
            var request = new FolderRequest(folder);
            var requestContent = JsonConvert.SerializeObject(request);
            var requestMessage = new HttpRequestMessage
            {
                Method = folder.Id == null ? HttpMethod.Post : HttpMethod.Put,
                RequestUri = new Uri(_apiService.Client.BaseAddress, folder.Id == null ? "/folders" : string.Concat("/folders/", folder.Id)),
                Content = new StringContent(requestContent, Encoding.UTF8, "application/json")
            };
            requestMessage.Headers.Add("Authorization", string.Concat("Bearer ", _authService.Token));

            var response = await _apiService.Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await _apiService.HandleErrorAsync<FolderResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<FolderResponse>(responseContent);
            var data = new FolderData(responseObj, _authService.UserId);

            if(folder.Id == null)
            {
                await CreateAsync(data);
                folder.Id = responseObj.Id;
            }
            else
            {
                await ReplaceAsync(data);
            }

            return ApiResult<FolderResponse>.Success(responseObj, response.StatusCode);
        }
    }
}
