using static Android.App.Assist.AssistStructure;
using Android.App.Assist;
using Bit.App;

namespace Bit.Android.Autofill
{
    public class Parser
    {
        private readonly AssistStructure _structure;
        private string _uri;
        private string _packageName;

        public Parser(AssistStructure structure)
        {
            _structure = structure;
        }

        public FieldCollection FieldCollection { get; private set; } = new FieldCollection();
        public string Uri
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(_uri))
                {
                    return _uri;
                }

                if(string.IsNullOrWhiteSpace(PackageName))
                {
                    _uri = null;
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

        public void Parse()
        {
            for(var i = 0; i < _structure.WindowNodeCount; i++)
            {
                var node = _structure.GetWindowNodeAt(i);
                ParseNode(node.RootViewNode);
            }
        }

        private void ParseNode(ViewNode node)
        {
            var hints = node.GetAutofillHints();
            var isEditText = node.ClassName == "android.widget.EditText";
            if(isEditText || (hints?.Length ?? 0) > 0)
            {
                if(PackageName == null)
                {
                    PackageName = node.IdPackage;
                }
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
    }
}