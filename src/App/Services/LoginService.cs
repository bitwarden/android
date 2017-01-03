using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Api;
using Bit.App.Models.Data;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class LoginService : ILoginService
    {
        private readonly ILoginRepository _loginRepository;
        private readonly IAuthService _authService;
        private readonly ILoginApiRepository _loginApiRepository;

        public LoginService(
            ILoginRepository loginRepository,
            IAuthService authService,
            ILoginApiRepository loginApiRepository)
        {
            _loginRepository = loginRepository;
            _authService = authService;
            _loginApiRepository = loginApiRepository;
        }

        public async Task<Login> GetByIdAsync(string id)
        {
            var data = await _loginRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var login = new Login(data);
            return login;
        }

        public async Task<IEnumerable<Login>> GetAllAsync()
        {
            var data = await _loginRepository.GetAllByUserIdAsync(_authService.UserId);
            var logins = data.Select(f => new Login(f));
            return logins;
        }

        public async Task<IEnumerable<Login>> GetAllAsync(bool favorites)
        {
            var data = await _loginRepository.GetAllByUserIdAsync(_authService.UserId, favorites);
            var logins = data.Select(f => new Login(f));
            return logins;
        }

        public async Task<ApiResult<LoginResponse>> SaveAsync(Login login)
        {
            ApiResult<LoginResponse> response = null;
            var request = new LoginRequest(login);

            if(login.Id == null)
            {
                response = await _loginApiRepository.PostAsync(request);
            }
            else
            {
                response = await _loginApiRepository.PutAsync(login.Id, request);
            }

            if(response.Succeeded)
            {
                var data = new LoginData(response.Result, _authService.UserId);
                if(login.Id == null)
                {
                    await _loginRepository.InsertAsync(data);
                    login.Id = data.Id;
                }
                else
                {
                    await _loginRepository.UpdateAsync(data);
                }
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden 
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }

        public async Task<ApiResult> DeleteAsync(string id)
        {
            var response = await _loginApiRepository.DeleteAsync(id);
            if(response.Succeeded)
            {
                await _loginRepository.DeleteAsync(id);
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
