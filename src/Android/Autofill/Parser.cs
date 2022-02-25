using static Android.App.Assist.AssistStructure;
using Android.App.Assist;
using System.Collections.Generic;
using Bit.Core;
using Android.Content;
using Bit.Core.Abstractions;
using System.Threading.Tasks;
using Android.OS;

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
        private string _website;

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
                if (!string.IsNullOrWhiteSpace(_uri))
                {
                    return _uri;
                }
                var websiteNull = string.IsNullOrWhiteSpace(Website);
                if (websiteNull && string.IsNullOrWhiteSpace(PackageName))
                {
                    _uri = null;
                }
                else if (!websiteNull)
                {
                    _uri = Website;
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
                if (string.IsNullOrWhiteSpace(value))
                {
                    _packageName = _uri = null;
                }
                _packageName = value;
            }
        }

        public string Website
        {
            get => _website;
            set
            {
                if (string.IsNullOrWhiteSpace(value))
                {
                    _website = _uri = null;
                }
                _website = value;
            }
        }

        public async Task<bool> ShouldAutofillAsync(IStateService stateService)
        {
            var fillable = !string.IsNullOrWhiteSpace(Uri) && !AutofillHelpers.BlacklistedUris.Contains(Uri) &&
                FieldCollection != null && FieldCollection.Fillable;
            if (fillable)
            {
                var blacklistedUris = await stateService.GetAutofillBlacklistedUrisAsync();
                if (blacklistedUris != null && blacklistedUris.Count > 0)
                {
                    fillable = !new HashSet<string>(blacklistedUris).Contains(Uri);
                }
            }
            return fillable;
        }

        public void Parse()
        {
            string titlePackageId = null;
            for (var i = 0; i < _structure.WindowNodeCount; i++)
            {
                var node = _structure.GetWindowNodeAt(i);
                if (i == 0)
                {
                    titlePackageId = GetTitlePackageId(node);
                }
                ParseNode(node.RootViewNode);
            }
            if (string.IsNullOrWhiteSpace(PackageName) && string.IsNullOrWhiteSpace(Website))
            {
                PackageName = titlePackageId;
            }
            if (!AutofillHelpers.TrustedBrowsers.Contains(PackageName) &&
                !AutofillHelpers.CompatBrowsers.Contains(PackageName))
            {
                Website = null;
            }
        }

        private void ParseNode(ViewNode node)
        {
            SetPackageAndDomain(node);
            var hints = node.GetAutofillHints();
            var isEditText = node.ClassName == "android.widget.EditText" || node?.HtmlInfo?.Tag == "input";
            if (isEditText || (hints?.Length ?? 0) > 0)
            {
                FieldCollection.Add(new Field(node));
            }
            else
            {
                FieldCollection.IgnoreAutofillIds.Add(node.AutofillId);
            }

            for (var i = 0; i < node.ChildCount; i++)
            {
                ParseNode(node.GetChildAt(i));
            }
        }

        private void SetPackageAndDomain(ViewNode node)
        {
            if (string.IsNullOrWhiteSpace(PackageName) && !string.IsNullOrWhiteSpace(node.IdPackage) &&
                !_excludedPackageIds.Contains(node.IdPackage))
            {
                PackageName = node.IdPackage;
            }
            if (string.IsNullOrWhiteSpace(Website) && !string.IsNullOrWhiteSpace(node.WebDomain))
            {
                var scheme = "http";
                if ((int)Build.VERSION.SdkInt >= 28)
                {
                    scheme = node.WebScheme;
                }
                Website = string.Format("{0}://{1}", scheme, node.WebDomain);
            }
        }

        private string GetTitlePackageId(WindowNode node)
        {
            if (node != null && !string.IsNullOrWhiteSpace(node.Title))
            {
                var slashPosition = node.Title.IndexOf('/');
                if (slashPosition > -1)
                {
                    var packageId = node.Title.Substring(0, slashPosition);
                    if (packageId.Contains("."))
                    {
                        return packageId;
                    }
                }
            }
            return null;
        }
    }
}
