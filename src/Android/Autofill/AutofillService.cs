using System;
using System.Collections.Generic;
using System.Linq;
using Android;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Service.Autofill;
using Android.Widget;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Services;
using Bit.Core.Utilities;

namespace Bit.Droid.Autofill
{
    [Service(Permission = Manifest.Permission.BindAutofillService, Label = "Bitwarden", Exported = true)]
    [IntentFilter(new string[] { "android.service.autofill.AutofillService" })]
    [MetaData("android.autofill", Resource = "@xml/autofillservice")]
    [Register("com.x8bit.bitwarden.Autofill.AutofillService")]
    public class AutofillService : Android.Service.Autofill.AutofillService
    {
        private ICipherService _cipherService;
        private IVaultTimeoutService _vaultTimeoutService;
        private IPolicyService _policyService;
        private IStateService _stateService;
        private LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");
        private IUserVerificationService _userVerificationService;

        public async override void OnFillRequest(FillRequest request, CancellationSignal cancellationSignal,
            FillCallback callback)
        {
            try
            {
                var structure = request.FillContexts?.LastOrDefault()?.Structure;
                if (structure == null)
                {
                    return;
                }

                var parser = new Parser(structure, ApplicationContext);
                parser.Parse();

                if (_stateService == null)
                {
                    _stateService = ServiceContainer.Resolve<IStateService>("stateService");
                }

                var shouldAutofill = await parser.ShouldAutofillAsync(_stateService);
                if (!shouldAutofill)
                {
                    return;
                }

                var inlineAutofillEnabled = await _stateService.GetInlineAutofillEnabledAsync() ?? true;

                if (_vaultTimeoutService == null)
                {
                    _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
                }

                List<FilledItem> items = null;
                await _vaultTimeoutService.CheckVaultTimeoutAsync();
                var locked = await _vaultTimeoutService.IsLockedAsync();
                if (!locked)
                {
                    _cipherService ??= ServiceContainer.Resolve<ICipherService>();
                    _userVerificationService ??=  ServiceContainer.Resolve<IUserVerificationService>();
                    items = await AutofillHelpers.GetFillItemsAsync(parser, _cipherService, _userVerificationService);
                }

                // build response
                var response = AutofillHelpers.CreateFillResponse(parser, items, locked, inlineAutofillEnabled, request);
                var disableSavePrompt = await _stateService.GetAutofillDisableSavePromptAsync();
                if (!disableSavePrompt.GetValueOrDefault())
                {
                    AutofillHelpers.AddSaveInfo(parser, request, response, parser.FieldCollection);
                }
                callback.OnSuccess(response.Build());
            }
            catch (Exception e)
            {
                _logger.Value.Exception(e);
            }
        }

        public async override void OnSaveRequest(SaveRequest request, SaveCallback callback)
        {
            try
            {
                var structure = request.FillContexts?.LastOrDefault()?.Structure;
                if (structure == null)
                {
                    return;
                }

                if (_stateService == null)
                {
                    _stateService = ServiceContainer.Resolve<IStateService>("stateService");
                }

                var disableSavePrompt = await _stateService.GetAutofillDisableSavePromptAsync();
                if (disableSavePrompt.GetValueOrDefault())
                {
                    return;
                }

                _policyService ??= ServiceContainer.Resolve<IPolicyService>("policyService");

                var personalOwnershipPolicyApplies = await _policyService.PolicyAppliesToUser(PolicyType.PersonalOwnership);
                if (personalOwnershipPolicyApplies)
                {
                    return;
                }

                var parser = new Parser(structure, ApplicationContext);
                parser.Parse();

                var savedItem = parser.FieldCollection.GetSavedItem();
                if (savedItem == null)
                {
                    Toast.MakeText(this, "Unable to save this form.", ToastLength.Short).Show();
                    return;
                }

                var intent = new Intent(this, typeof(MainActivity));
                intent.SetFlags(ActivityFlags.NewTask | ActivityFlags.ClearTop);
                intent.PutExtra("autofillFramework", true);
                intent.PutExtra("autofillFrameworkSave", true);
                intent.PutExtra("autofillFrameworkType", (int)savedItem.Type);
                switch (savedItem.Type)
                {
                    case CipherType.Login:
                        intent.PutExtra("autofillFrameworkName", parser.Uri
                            .Replace(Core.Constants.AndroidAppProtocol, string.Empty)
                            .Replace("https://", string.Empty)
                            .Replace("http://", string.Empty));
                        intent.PutExtra("autofillFrameworkUri", parser.Uri);
                        intent.PutExtra("autofillFrameworkUsername", savedItem.Login.Username);
                        intent.PutExtra("autofillFrameworkPassword", savedItem.Login.Password);
                        break;
                    case CipherType.Card:
                        intent.PutExtra("autofillFrameworkCardName", savedItem.Card.Name);
                        intent.PutExtra("autofillFrameworkCardNumber", savedItem.Card.Number);
                        intent.PutExtra("autofillFrameworkCardExpMonth", savedItem.Card.ExpMonth);
                        intent.PutExtra("autofillFrameworkCardExpYear", savedItem.Card.ExpYear);
                        intent.PutExtra("autofillFrameworkCardCode", savedItem.Card.Code);
                        break;
                    default:
                        Toast.MakeText(this, "Unable to save this type of form.", ToastLength.Short).Show();
                        return;
                }
                StartActivity(intent);
            }
            catch (Exception e)
            {
                _logger.Value.Exception(e);
            }
        }
    }
}
