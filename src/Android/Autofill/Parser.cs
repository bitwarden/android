using static Android.App.Assist.AssistStructure;
using Android.App.Assist;
using System.Collections.Generic;
using Bit.Core;
using Android.Content;

namespace Bit.Droid.Autofill
{
    public class Parser
    {
        public static HashSet<string> _excludedPackageIds = new HashSet<string>
        {
            "android"
        };
        private readonly AssistStructure _structure;
        private string _uri;
        private string _packageName;
        private string _webDomain;

        public Parser(AssistStructure structure, Context applicationContext)
        {
            _structure = structure;
            ApplicationContext = applicationContext;
        }

        public Context ApplicationContext { get; set; }
        public FieldCollection FieldCollection { get; private set; } = new FieldCollection();

        public string Uri
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(_uri))
                {
                    return _uri;
                }
                var webDomainNull = string.IsNullOrWhiteSpace(WebDomain);
                if(webDomainNull && string.IsNullOrWhiteSpace(PackageName))
                {
                    _uri = null;
                }
                else if(!webDomainNull)
                {
                    _uri = string.Concat("http://", WebDomain);
                }
                else
                {
                    _uri = string.Concat(Constants.AndroidAppProtocol, PackageName);
                }
                return _uri;
            }
        }

        public string PackageName
        {
            get => _packageName;
            set
            {
                if(string.IsNullOrWhiteSpace(value))
                {
                    _packageName = _uri = null;
                }
                _packageName = value;
            }
        }

        public string WebDomain
        {
            get => _webDomain;
            set
            {
                if(string.IsNullOrWhiteSpace(value))
                {
                    _webDomain = _uri = null;
                }
                _webDomain = value;
            }
        }

        public bool ShouldAutofill => !string.IsNullOrWhiteSpace(Uri) &&
            !AutofillHelpers.BlacklistedUris.Contains(Uri) && FieldCollection != null && FieldCollection.Fillable;

        public void Parse()
        {
            for(var i = 0; i < _structure.WindowNodeCount; i++)
            {
                var node = _structure.GetWindowNodeAt(i);
                ParseNode(node.RootViewNode);
            }
            if(!AutofillHelpers.TrustedBrowsers.Contains(PackageName) &&
                !AutofillHelpers.CompatBrowsers.Contains(PackageName))
            {
                WebDomain = null;
            }
        }

        private void ParseNode(ViewNode node)
        {
            SetPackageAndDomain(node);
            var hints = node.GetAutofillHints();
            var isEditText = node.ClassName == "android.widget.EditText" || node?.HtmlInfo?.Tag == "input";
            if(isEditText || (hints?.Length ?? 0) > 0)
            {
                FieldCollection.Add(new Field(node));
            }
            else
            {
                FieldCollection.IgnoreAutofillIds.Add(node.AutofillId);
            }

            for(var i = 0; i < node.ChildCount; i++)
            {
                ParseNode(node.GetChildAt(i));
            }
        }

        private void SetPackageAndDomain(ViewNode node)
        {
            if(string.IsNullOrWhiteSpace(PackageName) && !string.IsNullOrWhiteSpace(node.IdPackage) &&
                !_excludedPackageIds.Contains(node.IdPackage))
            {
                PackageName = node.IdPackage;
            }
            if(string.IsNullOrWhiteSpace(WebDomain) && !string.IsNullOrWhiteSpace(node.WebDomain))
            {
                WebDomain = node.WebDomain;
            }
        }
    }
}
