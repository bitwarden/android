using static Android.App.Assist.AssistStructure;
using Android.App.Assist;
using Bit.App;

namespace Bit.Android.Autofill
{
    public class Parser
    {
        private readonly AssistStructure _structure;
        private string _uri;

        public Parser(AssistStructure structure)
        {
            _structure = structure;
        }

        public FieldCollection FieldCollection { get; private set; } = new FieldCollection();
        public string Uri
        {
            get => _uri;
            set
            {
                if(string.IsNullOrWhiteSpace(value))
                {
                    _uri = null;
                    return;
                }

                _uri = string.Concat(Constants.AndroidAppProtocol, value);
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
                if(Uri == null)
                {
                    Uri = node.IdPackage;
                }
                FieldCollection.Add(new Field(node));
            }

            for(var i = 0; i < node.ChildCount; i++)
            {
                ParseNode(node.GetChildAt(i));
            }
        }
    }
}