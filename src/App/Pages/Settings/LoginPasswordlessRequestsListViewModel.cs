using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class LoginPasswordlessRequestsListViewModel : BaseViewModel
    {
        private readonly IAuthService _authService;
        private readonly IStateService _stateService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ILogger _logger;
        private List<PasswordlessLoginResponse> _loginRequestsList;

        public LoginPasswordlessRequestsListViewModel()
        {
            _authService = ServiceContainer.Resolve<IAuthService>();
            _stateService = ServiceContainer.Resolve<IStateService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _logger = ServiceContainer.Resolve<ILogger>();

            PageTitle = AppResources.LogInWithAnotherDevice;

            AcceptRequestCommand = new AsyncCommand<PasswordlessLoginResponse>((request) => PasswordlessLoginAsync(request, true),
               onException: ex => HandleException(ex, _deviceActionService, _platformUtilsService, _logger),
               allowsMultipleExecutions: false);

            RejectRequestCommand = new AsyncCommand<PasswordlessLoginResponse>((request) => PasswordlessLoginAsync(request, false),
                onException: ex => HandleException(ex, _deviceActionService, _platformUtilsService, _logger),
                allowsMultipleExecutions: false);
        }

        public List<PasswordlessLoginResponse> LoginRequestsList
        {
            get => _loginRequestsList;
            set => SetProperty(ref _loginRequestsList, value);
        }

        public AsyncCommand<PasswordlessLoginResponse> AcceptRequestCommand { get; }
        public AsyncCommand<PasswordlessLoginResponse> RejectRequestCommand { get; }

        public async Task InitAsync()
        {
            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
            LoginRequestsList = await _authService.GetPasswordlessLoginRequestsAsync();
            await _deviceActionService.HideLoadingAsync();
        }

        private async Task PasswordlessLoginAsync(PasswordlessLoginResponse request, bool approveRequest)
        {
            if (request.IsExpired)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.LoginRequestHasAlreadyExpired);
                await Page.Navigation.PopModalAsync();
                return;
            }

            var loginRequestData = await _authService.GetPasswordlessLoginRequestByIdAsync(request.Id);
            if (loginRequestData.RequestApproved.HasValue && loginRequestData.ResponseDate.HasValue)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.ThisRequestIsNoLongerValid);
                await Page.Navigation.PopModalAsync();
                return;
            }

            await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
            await _authService.PasswordlessLoginAsync(request.Id, request.PublicKey, approveRequest);
            await _deviceActionService.HideLoadingAsync();
            await Page.Navigation.PopModalAsync();
            _platformUtilsService.ShowToast("info", null, approveRequest ? AppResources.LogInAccepted : AppResources.LogInDenied);
        }
    }
}

