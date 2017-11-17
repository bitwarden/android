using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.Support.V7.App;
using Android.Views.Autofill;
using Android.App.Assist;
using XLabs.Ioc;
using Bit.App.Abstractions;
using Bit.App.Resources;

namespace Bit.Android.Autofill
{
    [Activity(Label = "AuthActivity")]
    public class AuthActivity : AppCompatActivity
    {
        private EditText _masterPassword;
        private Intent _replyIntent = null;

        protected override void OnCreate(Bundle state)
        {
            base.OnCreate(state);
            SetContentView(Resource.Layout.autofill_authactivity);
            var masterLoginLabel = FindViewById(Resource.Id.master_login_header) as TextView;
            masterLoginLabel.Text = AppResources.VerifyMasterPassword;


            var masterPasswordLabel = FindViewById(Resource.Id.password_label) as TextView;
            masterPasswordLabel.Text = AppResources.MasterPassword;

            _masterPassword = FindViewById(Resource.Id.master_password) as EditText;

            var loginButton = FindViewById(Resource.Id.login) as TextView;
            loginButton.Text = AppResources.LogIn;
            loginButton.Click += (sender, e) => {
                Login();
            };


            var cancelButton = FindViewById(Resource.Id.cancel) as TextView;
            cancelButton.Text = AppResources.Cancel;
            cancelButton.Click += (sender, e) => {
                _replyIntent = null;
                Finish();
            };
        }

        public override void Finish()
        {
            if(_replyIntent != null)
            {
                SetResult(Result.Ok, _replyIntent);
            }
            else
            {
                SetResult(Result.Canceled);
            }

            base.Finish();
        }
        
        private void Login()
        {
            var password = _masterPassword.Text;
            if(true) // Check password
            {
                Success();
            }
            else
            {
                Toast.MakeText(this, "Password incorrect", ToastLength.Short).Show();
                _replyIntent = null;
            }

            Finish();
        }

        private async void Success()
        {
            var structure = Intent.GetParcelableExtra(AutofillManager.ExtraAssistStructure) as AssistStructure;
            if(structure == null)
            {
                _replyIntent = null;
                return;
            }

            var parser = new Parser(structure);
            parser.ParseForFill();
            if(!parser.FieldCollection.Fields.Any() || string.IsNullOrWhiteSpace(parser.Uri))
            {
                _replyIntent = null;
                return;
            }

            var items = await AutofillHelpers.GetFillItemsAsync(Resolver.Resolve<ICipherService>(), parser.Uri);
            if(!items.Any())
            {
                _replyIntent = null;
                return;
            }

            var response = AutofillHelpers.BuildFillResponse(this, parser.FieldCollection, items);
            _replyIntent = new Intent();
            _replyIntent.PutExtra(AutofillManager.ExtraAuthenticationResult, response);
        }
    }
}