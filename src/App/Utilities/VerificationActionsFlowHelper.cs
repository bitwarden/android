using System;
using System.Collections.Generic;
using System.Globalization;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Pages.Accounts;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Newtonsoft.Json;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    //public interface IVerificationFlowHandler
    //{
    //    Task Handle();
    //}

    //public abstract class VerificationFlowHandler : IVerificationFlowHandler
    //{
    //    IVerificationFlowHandler _nextHandler;

    //    public VerificationFlowHandler(IVerificationFlowHandler nextHandler)
    //    {
    //        _nextHandler = nextHandler;
    //    }

    //    public virtual Task Handle()
    //    {
    //        return _nextHandler.Handle();
    //    }
    //}

    //public class MasterPasswordVerificationFlowHandler : VerificationFlowHandler
    //{
    //    readonly IUserVerificationService _userVerificationService;

    //    public MasterPasswordVerificationFlowHandler(IVerificationFlowHandler nextHandler)
    //        : base(nextHandler)
    //    {
    //    }

    //    public override async Task Handle()
    //    {
    //        if (!await _userVerificationService.VerifyUser(Secret, Core.Enums.VerificationType.MasterPassword))
    //        {
    //            return;
    //        }

    //        await base.Handle();
    //    }
    //}

    //public class OTPVerificationFlowHandler : VerificationFlowHandler
    //{
    //    readonly IUserVerificationService _userVerificationService;

    //    public OTPVerificationFlowHandler(IVerificationFlowHandler nextHandler)
    //        : base(nextHandler)
    //    {
    //    }

    //    public override async Task Handle()
    //    {
    //        if (!await _userVerificationService.VerifyUser(Secret, Core.Enums.VerificationType.OTP))
    //        {
    //            return;
    //        }

    //        await base.Handle();
    //    }
    //}

    //public class VerificationFlowExecutioner
    //{
    //    IVerificationFlowHandler _handler;

    //    public VerificationFlowExecutioner(IVerificationFlowHandler handler)
    //    {
    //        _handler = handler;
    //    }

    //    public async Task ExecuteAsync()
    //    {
    //        if (!await _handler.Handle())
    //        {
    //            return;
    //        }


    //    }
    //}

    public interface IVerificationActionsFlowHelper
    {
        IVerificationActionsFlowHelper Configure(VerificationFlowAction action, IActionFlowParmeters parameters = null);
        IActionFlowParmeters GetParameters();
        Task ValidateAndExecuteAsync();
        Task ExecuteAsync(IActionFlowParmeters parameters);
    }

    public interface IActionFlowParmeters
    {
        string Secret { get; set; }
    }

    public class DefaultActionFlowParameters : IActionFlowParmeters
    {
        public string Secret { get; set; }
    }

    public interface IActionFlowExecutioner
    {
        Task Execute(IActionFlowParmeters parameters);
    }

    public enum VerificationFlowAction
    {
        ExportVault,
        DeleteAccount
    }

    /// <summary>
    /// Verifies and execute actions on the corresponding order.
    /// 
    /// Use: From the caller
    /// - Configure it on <see cref="Configure(VerificationFlowAction, IActionFlowParmeters)"/>
    /// - Call <see cref="ValidateAndExecuteAsync"/>
    /// 
    /// For every <see cref="VerificationFlowAction"/> we need an implementation of <see cref="IActionFlowExecutioner"/>
    /// and it to be configured in the inner dictionary.
    /// Also, inherit from <see cref="DefaultActionFlowParameters"/> if more custom parameters are needed for the executioner.
    /// </summary>
    public class VerificationActionsFlowHelper : IVerificationActionsFlowHelper
    {
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private VerificationFlowAction? _action;
        private IActionFlowParmeters _parameters;

        private readonly Dictionary<VerificationFlowAction, IActionFlowExecutioner> _actionExecutionerDictionary = new Dictionary<VerificationFlowAction, IActionFlowExecutioner>();

        public VerificationActionsFlowHelper(IKeyConnectorService keyConnectorService,
            IPasswordRepromptService passwordRepromptService,
            IPlatformUtilsService platformUtilsService)
        {
            //_keyConnectorService = ServiceContainer.Resolve<IKeyConnectorService>("keyConnectorService");
            //_passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
            //_platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _keyConnectorService = keyConnectorService;
            _passwordRepromptService = passwordRepromptService;
            _platformUtilsService = platformUtilsService;

            _actionExecutionerDictionary.Add(VerificationFlowAction.DeleteAccount, ServiceContainer.Resolve<IDeleteAccountActionFlowExecutioner>("deleteAccountActionFlowExecutioner"));
        }

        public IVerificationActionsFlowHelper Configure(VerificationFlowAction action, IActionFlowParmeters parameters = null)
        {
            _action = action;
            _parameters = parameters;

            return this;
        }

        public IActionFlowParmeters GetParameters()
        {
            if (_parameters is null)
            {
                _parameters = new DefaultActionFlowParameters();
            }

            return _parameters;
        }

        public async Task ValidateAndExecuteAsync()
        {
            var verificationType = await _keyConnectorService.GetUsesKeyConnector()
                ? VerificationType.OTP
                : VerificationType.MasterPassword;

            switch (verificationType)
            {
                case VerificationType.MasterPassword:
                    var (password, valid) = await _passwordRepromptService.ShowPasswordPromptAndGetItAsync();
                    if (!valid)
                    {
                        await _platformUtilsService.ShowDialogAsync(AppResources.InvalidMasterPassword);
                        return;
                    }

                    _parameters.Secret = password;
                    await ExecuteAsync(_parameters);
                    break;
                case VerificationType.OTP:
                    await Application.Current.MainPage.Navigation.PushModalAsync(new NavigationPage(new VerificationCodePage()));
                    break;
                default:
                    throw new NotImplementedException($"There is no implementation for {verificationType}");
            }
        }

        /// <summary>
        /// Executes the action with the given parameters after we have gotten the verification secret
        /// </summary>
        public async Task ExecuteAsync(IActionFlowParmeters parameters)
        {
            if (!_action.HasValue)
            {
                // this should never happen
                throw new InvalidOperationException("A problem occurred while getting the action value after validation");
            }

            if (!_actionExecutionerDictionary.TryGetValue(_action.Value, out var executioner))
            {
                throw new InvalidOperationException($"There is no executioner for {_action}");
            }

            await executioner.Execute(GetParameters());
        }
    }
}
