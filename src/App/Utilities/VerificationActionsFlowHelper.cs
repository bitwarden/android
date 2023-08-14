using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Pages.Accounts;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public interface IVerificationActionsFlowHelper
    {
        IVerificationActionsFlowHelper Configure(VerificationFlowAction action,
            IActionFlowParmeters parameters = null,
            string verificatioCodeMainActionText = null,
            bool isVerificationCodeMainActionStyleDanger = false);
        IActionFlowParmeters GetParameters();
        Task ValidateAndExecuteAsync();
        Task ExecuteAsync(IActionFlowParmeters parameters);
    }

    public interface IActionFlowParmeters
    {
        VerificationType VerificationType { get; set; }

        string Secret { get; set; }
    }

    public class DefaultActionFlowParameters : IActionFlowParmeters
    {
        public VerificationType VerificationType { get; set; }

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
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly ICryptoService _cryptoService;
        private readonly IUserVerificationService _userVerificationService;

        private VerificationFlowAction? _action;
        private IActionFlowParmeters _parameters;
        private string _verificationCodeMainActionText;
        private bool _isVerificationCodeMainActionStyleDanger;

        private readonly Dictionary<VerificationFlowAction, IActionFlowExecutioner> _actionExecutionerDictionary = new Dictionary<VerificationFlowAction, IActionFlowExecutioner>();

        public VerificationActionsFlowHelper(
            IPasswordRepromptService passwordRepromptService,
            ICryptoService cryptoService,
            IUserVerificationService userVerificationService)
        {
            _passwordRepromptService = passwordRepromptService;
            _cryptoService = cryptoService;
            _userVerificationService = userVerificationService;

            _actionExecutionerDictionary.Add(VerificationFlowAction.DeleteAccount, ServiceContainer.Resolve<IDeleteAccountActionFlowExecutioner>("deleteAccountActionFlowExecutioner"));
        }

        public IVerificationActionsFlowHelper Configure(VerificationFlowAction action,
            IActionFlowParmeters parameters = null,
            string verificationCodeMainActionText = null,
            bool isVerificationCodeMainActionStyleDanger = false)
        {
            _action = action;
            _parameters = parameters;
            _verificationCodeMainActionText = verificationCodeMainActionText;
            _isVerificationCodeMainActionStyleDanger = isVerificationCodeMainActionStyleDanger;

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
            var verificationType = await _userVerificationService.HasMasterPasswordAsync(true)
                ? VerificationType.MasterPassword
                : VerificationType.OTP;

            switch (verificationType)
            {
                case VerificationType.MasterPassword:
                    var (password, valid) = await _passwordRepromptService.ShowPasswordPromptAndGetItAsync();
                    if (!valid)
                    {
                        return;
                    }

                    var parameters = GetParameters();
                    parameters.Secret = await _cryptoService.HashMasterKeyAsync(password, null);
                    parameters.VerificationType = VerificationType.MasterPassword;
                    await ExecuteAsync(parameters);
                    break;
                case VerificationType.OTP:
                    await Application.Current.MainPage.Navigation.PushModalAsync(new NavigationPage(
                        new VerificationCodePage(_verificationCodeMainActionText, _isVerificationCodeMainActionStyleDanger)));
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
